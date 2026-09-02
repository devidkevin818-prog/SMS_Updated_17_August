package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.entity.*;
import com.bank.signaturemanagement.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UserApprovalService {
    private static final List<String> OPEN = List.of("PENDING_DGM_APPROVAL", "PENDING_GM_APPROVAL");
    private static final Set<String> PD_TARGET_ROLES = Set.of("PD", "DGM", "GM", "BRANCH");
    private final UserCreationRequestRepository requests;
    private final UserRepository users;
    private final RoleRepository roles;
    private final EmployeeRepository employees;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AccessControlService access;
    private final EmployeeNumberPolicyService employeeNumberPolicy;

    public UserApprovalService(UserCreationRequestRepository requests, UserRepository users,
                               RoleRepository roles, EmployeeRepository employees, BranchRepository branches,
                               PasswordEncoder encoder, AuditService audit, AccessControlService access,
                               EmployeeNumberPolicyService employeeNumberPolicy) {
        this.requests = requests; this.users = users; this.roles = roles; this.employees = employees;
        this.branches = branches; this.encoder = encoder; this.audit = audit; this.access = access;
        this.employeeNumberPolicy = employeeNumberPolicy;
    }

    @Transactional
    public boolean propose(UserForm form, String creator) {
        access.require(creator, "USER_PROPOSE");
        User actor = users.findByUsername(creator).orElseThrow();
        String actorRole = actor.getRole().getName();
        if (!Set.of("ADMIN", "PD").contains(actorRole)) throw new AccessDeniedException("Only Admin or PD may propose users");
        Role role = roles.findByName(form.getRoleName()).orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        if (!role.isActive()) throw new IllegalArgumentException("Selected role is inactive");
        if ("PD".equals(actorRole) && !PD_TARGET_ROLES.contains(role.getName())) {
            throw new IllegalArgumentException("PD can create only PD, DGM, GM, or Branch users");
        }
        String employeeNumber = employeeNumberPolicy.normalize(form.getEmployeeNumber());
        Employee employee = employees.findByEmployeeNumber(employeeNumber)
                .filter(Employee::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Employee ID does not match an active, approved employee record"));
        if (!employee.getEmployeeNumber().equals(employeeNumber)) throw new IllegalArgumentException("Invalid employee link");
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();
        requireActiveBranch(form.getBranchId());
        if (users.existsByUsername(username) || requests.existsByProposedUsernameAndStatusIn(username, OPEN)) {
            throw new IllegalArgumentException("Username already exists or is pending");
        }
        if (users.existsByEmail(email) || requests.existsByProposedEmailIgnoreCaseAndStatusIn(email, OPEN)) {
            throw new IllegalArgumentException("Email already exists or is pending");
        }
        if (users.existsByEmployeeNumber(employeeNumber) || requests.existsByProposedEmployeeNumberAndStatusIn(employeeNumber, OPEN)) {
            throw new IllegalArgumentException("Employee ID already has an account or pending request");
        }
        String scope = normalizeScope(form.getSignatureScope());
        boolean bootstrap = users.countByRoleNameAndActiveTrue(role.getName()) == 0 && "ADMIN".equals(actorRole);
        if (bootstrap) {
            User user = toUser(form, role, employeeNumber, scope, actor);
            users.saveAndFlush(user);
            audit.record(creator, "USER_BOOTSTRAP_CREATE", "USER", String.valueOf(user.getId()), null,
                    "SUCCESS", null, username, "First active user for role " + role.getName());
            return true;
        }
        UserCreationRequest request = new UserCreationRequest();
        request.setProposedBy(actor); request.setProposedUsername(username);
        request.setProposedPasswordHash(encoder.encode(form.getPassword()));
        request.setProposedFullName(form.getFullName().trim()); request.setProposedEmployeeNumber(employeeNumber);
        request.setProposedEmail(email); request.setProposedBranchId(form.getBranchId().trim());
        request.setProposedRole(role); request.setProposedScope(scope);
        requests.saveAndFlush(request);
        audit.record(creator, "USER_PROPOSE", "USER_REQUEST", String.valueOf(request.getId()), null,
                "SUCCESS", null, "PENDING_DGM_APPROVAL", role.getName());
        return false;
    }

    @Transactional(readOnly = true)
    public List<UserCreationRequest> pending(String level) {
        return requests.findByStatusOrderByCreatedAtAsc("DGM".equals(level)
                ? "PENDING_DGM_APPROVAL" : "PENDING_GM_APPROVAL");
    }

    @Transactional
    public void decide(Long id, String levelValue, String actionValue, String comment, String actorName) {
        String level = normalizeLevel(levelValue);
        String action = normalizeAction(actionValue);
        access.require(actorName, "DGM".equals(level) ? "APPROVE_DGM" : "APPROVE_GM");
        User actor = users.findByUsername(actorName).orElseThrow();
        if (!level.equals(actor.getRole().getName()) && !"ADMIN".equals(actor.getRole().getName())) {
            throw new AccessDeniedException("Wrong approval level");
        }
        UserCreationRequest request = requests.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        String expected = "PENDING_" + level + "_APPROVAL";
        if (!expected.equals(request.getStatus())) throw new IllegalArgumentException("Request is not awaiting " + level + " approval");
        LocalDateTime now = LocalDateTime.now();
        if ("DGM".equals(level)) { request.setDgmDecidedBy(actor); request.setDgmDecidedAt(now); request.setDgmComment(clean(comment)); }
        else { request.setGmDecidedBy(actor); request.setGmDecidedAt(now); request.setGmComment(clean(comment)); }
        if ("REJECT".equals(action)) {
            if (comment == null || comment.isBlank()) throw new IllegalArgumentException("Rejection reason is required");
            request.setStatus("REJECTED"); request.setRejectionReason(comment.trim()); request.setDecidedAt(now);
        } else if ("DGM".equals(level)) {
            request.setStatus("PENDING_GM_APPROVAL");
        } else {
            validateStillAvailable(request);
            try { users.saveAndFlush(toUser(request, actor)); }
            catch (DataIntegrityViolationException exception) {
                throw new IllegalArgumentException("Username, email, or employee ID became unavailable", exception);
            }
            request.setStatus("APPROVED"); request.setDecidedAt(now);
        }
        audit.record(actorName, "USER_" + level + "_" + action, "USER_REQUEST", id.toString(), null,
                "SUCCESS", expected, request.getStatus(), clean(comment));
    }

    private void validateStillAvailable(UserCreationRequest request) {
        if (users.existsByUsername(request.getProposedUsername())) throw new IllegalArgumentException("Username became unavailable");
        if (users.existsByEmail(request.getProposedEmail())) throw new IllegalArgumentException("Email became unavailable");
        if (users.existsByEmployeeNumber(request.getProposedEmployeeNumber())) throw new IllegalArgumentException("Employee ID became unavailable");
        employees.findByEmployeeNumber(request.getProposedEmployeeNumber()).filter(Employee::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Linked employee is no longer active"));
        requireActiveBranch(request.getProposedBranchId());
        if (!request.getProposedRole().isActive()) throw new IllegalArgumentException("Selected role is no longer active");
    }

    private User toUser(UserForm form, Role role, String employeeNumber, String scope, User creator) {
        User user = new User(); user.setUsername(form.getUsername().trim());
        user.setPasswordHash(encoder.encode(form.getPassword())); user.setFullName(form.getFullName().trim());
        user.setEmployeeNumber(employeeNumber); user.setEmail(form.getEmail().trim()); user.setBranchId(form.getBranchId().trim());
        user.setRole(role); user.setSignatureScope(scope); user.setActive(true); user.setApprovalStatus("APPROVED"); user.setCreatedBy(creator);
        return user;
    }

    private User toUser(UserCreationRequest request, User creator) {
        User user = new User(); user.setUsername(request.getProposedUsername()); user.setPasswordHash(request.getProposedPasswordHash());
        user.setFullName(request.getProposedFullName()); user.setEmployeeNumber(request.getProposedEmployeeNumber());
        user.setEmail(request.getProposedEmail()); user.setBranchId(request.getProposedBranchId()); user.setRole(request.getProposedRole());
        user.setSignatureScope(request.getProposedScope()); user.setApprovalStatus("APPROVED"); user.setActive(true); user.setCreatedBy(creator);
        return user;
    }

    private void requireActiveBranch(String value) {
        try {
            Branch branch = branches.findById(Long.valueOf(value)).orElseThrow();
            if (!branch.isActive()) throw new IllegalArgumentException("Selected branch is inactive");
        } catch (NumberFormatException | java.util.NoSuchElementException exception) {
            throw new IllegalArgumentException("Select a valid branch");
        }
    }

    private String normalizeScope(String value) {
        String scope = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("LOCAL", "FOREIGN", "BOTH").contains(scope)) throw new IllegalArgumentException("Invalid signature scope");
        return scope;
    }
    private String normalizeLevel(String value) {
        String level=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DGM","GM").contains(level)) throw new IllegalArgumentException("Invalid approval level");
        return level;
    }
    private String normalizeAction(String value) {
        String action=value==null?"":value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVE","REJECT").contains(action)) throw new IllegalArgumentException("Invalid approval action");
        return action;
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
