package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.entity.Branch;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.Role;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.entity.UserCreationRequest;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserCreationRequestRepository;
import com.bank.signaturemanagement.repository.UserRepository;

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

    private static final String STATUS_PENDING_LEVEL_1 =
            "PENDING_LEVEL_1_CHECKER_APPROVAL";

    private static final String STATUS_PENDING_LEVEL_2 =
            "PENDING_LEVEL_2_CHECKER_APPROVAL";

    private static final String STATUS_APPROVED =
            "APPROVED";

    private static final String STATUS_REJECTED =
            "REJECTED";

    private static final String ROLE_ADMIN =
            "ADMIN";

    private static final String ROLE_MAKER =
            "MAKER";

    private static final String ROLE_LEVEL_1_CHECKER =
            "LEVEL_1_CHECKER";

    private static final String ROLE_LEVEL_2_CHECKER =
            "LEVEL_2_CHECKER";

    private static final List<String> OPEN = List.of(
            STATUS_PENDING_LEVEL_1,
            STATUS_PENDING_LEVEL_2
    );

    /*
     * Roles that a Maker is allowed to propose.
     */
    private static final Set<String> MAKER_TARGET_ROLES = Set.of(
            ROLE_MAKER,
            ROLE_LEVEL_1_CHECKER,
            ROLE_LEVEL_2_CHECKER,
            "BRANCH"
    );

    private final UserCreationRequestRepository requests;
    private final UserRepository users;
    private final RoleRepository roles;
    private final EmployeeRepository employees;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AccessControlService access;
    private final EmployeeNumberPolicyService employeeNumberPolicy;

    public UserApprovalService(
            UserCreationRequestRepository requests,
            UserRepository users,
            RoleRepository roles,
            EmployeeRepository employees,
            BranchRepository branches,
            PasswordEncoder encoder,
            AuditService audit,
            AccessControlService access,
            EmployeeNumberPolicyService employeeNumberPolicy
    ) {
        this.requests = requests;
        this.users = users;
        this.roles = roles;
        this.employees = employees;
        this.branches = branches;
        this.encoder = encoder;
        this.audit = audit;
        this.access = access;
        this.employeeNumberPolicy = employeeNumberPolicy;
    }

    @Transactional
    public boolean propose(
            UserForm form,
            String creator
    ) {
        access.require(
                creator,
                "USER_PROPOSE"
        );

        if (form == null) {
            throw new IllegalArgumentException(
                    "User information is required"
            );
        }

        User actor = requireUser(creator);

        String actorRole = requireRoleName(actor);

        if (!Set.of(
                ROLE_ADMIN,
                ROLE_MAKER
        ).contains(actorRole)) {
            throw new AccessDeniedException(
                    "Only Admin or Maker may propose users"
            );
        }

        validateRequiredText(
                form.getRoleName(),
                "Role is required"
        );

        Role role = roles.findByName(
                        form.getRoleName().trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid role"
                        )
                );

        if (!role.isActive()) {
            throw new IllegalArgumentException(
                    "Selected role is inactive"
            );
        }

        if (ROLE_MAKER.equals(actorRole)
                && !MAKER_TARGET_ROLES.contains(
                role.getName()
        )) {
            throw new IllegalArgumentException(
                    "Maker can create only Maker, Level 1 Checker, "
                            + "Level 2 Checker, or Branch users"
            );
        }

        validateRequiredText(
                form.getUsername(),
                "Username is required"
        );

        validateRequiredText(
                form.getPassword(),
                "Password is required"
        );

        validateRequiredText(
                form.getFullName(),
                "Full name is required"
        );

        validateRequiredText(
                form.getEmail(),
                "Email is required"
        );

        String employeeNumber =
                employeeNumberPolicy.normalize(
                        form.getEmployeeNumber()
                );

        Employee employee =
                employees.findByEmployeeNumber(employeeNumber)
                        .filter(Employee::isActive)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee ID does not match an active, "
                                                + "approved employee record"
                                )
                        );

        if (!employeeNumber.equals(
                employee.getEmployeeNumber()
        )) {
            throw new IllegalArgumentException(
                    "Invalid employee link"
            );
        }

        String username =
                form.getUsername().trim();

        String email =
                form.getEmail().trim();

        Long branchId =
                form.getBranchId();

        requireActiveBranch(branchId);

        if (users.existsByUsername(username)
                || requests
                .existsByProposedUsernameAndStatusIn(
                        username,
                        OPEN
                )) {
            throw new IllegalArgumentException(
                    "Username already exists or is pending"
            );
        }

        if (users.existsByEmail(email)
                || requests
                .existsByProposedEmailIgnoreCaseAndStatusIn(
                        email,
                        OPEN
                )) {
            throw new IllegalArgumentException(
                    "Email already exists or is pending"
            );
        }

        if (users.existsByEmployeeNumber(employeeNumber)
                || requests
                .existsByProposedEmployeeNumberAndStatusIn(
                        employeeNumber,
                        OPEN
                )) {
            throw new IllegalArgumentException(
                    "Employee ID already has an account or pending request"
            );
        }

        String scope =
                normalizeScope(
                        form.getSignatureScope()
                );

        boolean bootstrap =
                users.countByRoleNameAndActiveTrue(
                        role.getName()
                ) == 0
                        && ROLE_ADMIN.equals(actorRole);

        if (bootstrap) {
            User user = toUser(
                    form,
                    role,
                    employeeNumber,
                    scope,
                    actor
            );

            users.saveAndFlush(user);

            audit.record(
                    creator,
                    "USER_BOOTSTRAP_CREATE",
                    "USER",
                    String.valueOf(user.getId()),
                    null,
                    "SUCCESS",
                    null,
                    username,
                    "First active user for role "
                            + role.getName()
            );

            return true;
        }

        UserCreationRequest request =
                new UserCreationRequest();

        request.setProposedBy(actor);
        request.setProposedUsername(username);

        request.setProposedPasswordHash(
                encoder.encode(
                        form.getPassword()
                )
        );

        request.setProposedFullName(
                form.getFullName().trim()
        );

        request.setProposedEmployeeNumber(
                employeeNumber
        );

        request.setProposedEmail(email);

        /*
         * UserCreationRequest now has only one Long setter.
         */
        request.setProposedBranchId(branchId);

        request.setProposedRole(role);
        request.setProposedScope(scope);
        request.setStatus(STATUS_PENDING_LEVEL_1);
        request.setActive(true);

        requests.saveAndFlush(request);

        audit.record(
                creator,
                "USER_PROPOSE",
                "USER_REQUEST",
                String.valueOf(request.getId()),
                null,
                "SUCCESS",
                null,
                STATUS_PENDING_LEVEL_1,
                role.getName()
        );

        return false;
    }

    @Transactional(readOnly = true)
    public List<UserCreationRequest> pending(
            String level
    ) {
        String role =
                normalizeCheckerRole(level);

        String status =
                ROLE_LEVEL_1_CHECKER.equals(role)
                        ? STATUS_PENDING_LEVEL_1
                        : STATUS_PENDING_LEVEL_2;

        return requests.findByStatusOrderByCreatedAtAsc(
                status
        );
    }

    @Transactional
    public void decide(
            Long id,
            String levelValue,
            String actionValue,
            String comment,
            String actorName
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Request ID is required"
            );
        }

        String role =
                normalizeCheckerRole(levelValue);

        String action =
                normalizeAction(actionValue);

        /*
         * Existing permission keys are retained for database compatibility.
         */
        access.require(
                actorName,
                ROLE_LEVEL_1_CHECKER.equals(role)
                        ? "APPROVE_DGM"
                        : "APPROVE_GM"
        );

        User actor = requireUser(actorName);

        String actorRole = requireRoleName(actor);

        if (!role.equals(actorRole)
                && !ROLE_ADMIN.equals(actorRole)) {
            throw new AccessDeniedException(
                    "Wrong approval level"
            );
        }

        UserCreationRequest request =
                requests.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Request not found"
                                )
                        );

        String expectedStatus =
                ROLE_LEVEL_1_CHECKER.equals(role)
                        ? STATUS_PENDING_LEVEL_1
                        : STATUS_PENDING_LEVEL_2;

        if (!expectedStatus.equals(
                request.getStatus()
        )) {
            throw new IllegalArgumentException(
                    "Request is not awaiting "
                            + checkerLabel(role)
                            + " approval"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (ROLE_LEVEL_1_CHECKER.equals(role)) {
            request.setLevel1CheckerDecidedBy(actor);
            request.setLevel1CheckerDecidedAt(now);
            request.setLevel1CheckerComment(
                    clean(comment)
            );
        } else {
            request.setLevel2CheckerDecidedBy(actor);
            request.setLevel2CheckerDecidedAt(now);
            request.setLevel2CheckerComment(
                    clean(comment)
            );
        }

        if ("REJECT".equals(action)) {
            if (comment == null || comment.isBlank()) {
                throw new IllegalArgumentException(
                        "Rejection reason is required"
                );
            }

            request.setStatus(STATUS_REJECTED);
            request.setRejectionReason(
                    comment.trim()
            );
            request.setDecidedAt(now);
            request.setActive(false);

        } else if (ROLE_LEVEL_1_CHECKER.equals(role)) {
            request.setStatus(
                    STATUS_PENDING_LEVEL_2
            );

        } else {
            validateStillAvailable(request);

            try {
                User approvedUser =
                        toUser(
                                request,
                                actor
                        );

                users.saveAndFlush(approvedUser);

            } catch (DataIntegrityViolationException exception) {
                throw new IllegalArgumentException(
                        "Username, email, or employee ID became unavailable",
                        exception
                );
            }

            request.setStatus(STATUS_APPROVED);
            request.setDecidedAt(now);
            request.setActive(false);
        }

        requests.save(request);

        audit.record(
                actorName,
                "USER_" + role + "_" + action,
                "USER_REQUEST",
                String.valueOf(id),
                null,
                "SUCCESS",
                expectedStatus,
                request.getStatus(),
                clean(comment)
        );
    }

    private void validateStillAvailable(
            UserCreationRequest request
    ) {
        if (users.existsByUsername(
                request.getProposedUsername()
        )) {
            throw new IllegalArgumentException(
                    "Username became unavailable"
            );
        }

        if (users.existsByEmail(
                request.getProposedEmail()
        )) {
            throw new IllegalArgumentException(
                    "Email became unavailable"
            );
        }

        if (users.existsByEmployeeNumber(
                request.getProposedEmployeeNumber()
        )) {
            throw new IllegalArgumentException(
                    "Employee ID became unavailable"
            );
        }

        employees.findByEmployeeNumber(
                        request.getProposedEmployeeNumber()
                )
                .filter(Employee::isActive)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Linked employee is no longer active"
                        )
                );

        requireActiveBranch(
                request.getProposedBranchId()
        );

        if (request.getProposedRole() == null
                || !request.getProposedRole().isActive()) {
            throw new IllegalArgumentException(
                    "Selected role is no longer active"
            );
        }
    }

    private User toUser(
            UserForm form,
            Role role,
            String employeeNumber,
            String scope,
            User creator
    ) {
        User user = new User();

        user.setUsername(
                form.getUsername().trim()
        );

        user.setPasswordHash(
                encoder.encode(
                        form.getPassword()
                )
        );

        user.setFullName(
                form.getFullName().trim()
        );

        user.setEmployeeNumber(
                employeeNumber
        );

        user.setEmail(
                form.getEmail().trim()
        );

        user.setBranchId(
                form.getBranchId()
        );

        user.setRole(role);
        user.setSignatureScope(scope);
        user.setActive(true);
        user.setApprovalStatus(STATUS_APPROVED);
        user.setCreatedBy(creator);

        return user;
    }

    private User toUser(
            UserCreationRequest request,
            User creator
    ) {
        User user = new User();

        user.setUsername(
                request.getProposedUsername()
        );

        user.setPasswordHash(
                request.getProposedPasswordHash()
        );

        user.setFullName(
                request.getProposedFullName()
        );

        user.setEmployeeNumber(
                request.getProposedEmployeeNumber()
        );

        user.setEmail(
                request.getProposedEmail()
        );

        user.setBranchId(
                request.getProposedBranchId()
        );

        user.setRole(
                request.getProposedRole()
        );

        user.setSignatureScope(
                request.getProposedScope()
        );

        user.setApprovalStatus(STATUS_APPROVED);
        user.setActive(true);
        user.setCreatedBy(creator);

        return user;
    }

    private User requireUser(
            String username
    ) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        return users.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    private String requireRoleName(
            User user
    ) {
        if (user.getRole() == null
                || user.getRole().getName() == null
                || user.getRole().getName().isBlank()) {
            throw new AccessDeniedException(
                    "User does not have a valid role"
            );
        }

        return user.getRole().getName();
    }

    private void requireActiveBranch(
            Long value
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Select a valid branch"
            );
        }

        Branch branch =
                branches.findById(value)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Select a valid branch"
                                )
                        );

        if (!branch.isActive()) {
            throw new IllegalArgumentException(
                    "Selected branch is inactive"
            );
        }
    }

    private String normalizeScope(
            String value
    ) {
        String scope =
                value == null
                        ? ""
                        : value.trim()
                        .toUpperCase(Locale.ROOT);

        if (!Set.of(
                "LOCAL",
                "FOREIGN",
                "BOTH"
        ).contains(scope)) {
            throw new IllegalArgumentException(
                    "Invalid signature scope"
            );
        }

        return scope;
    }

    /*
     * DGM and GM remain accepted for existing controller routes.
     */
    private String normalizeCheckerRole(
            String value
    ) {
        String level =
                value == null
                        ? ""
                        : value.trim()
                        .toUpperCase(Locale.ROOT);

        return switch (level) {
            case "LEVEL_1_CHECKER", "DGM" ->
                    ROLE_LEVEL_1_CHECKER;

            case "LEVEL_2_CHECKER", "GM" ->
                    ROLE_LEVEL_2_CHECKER;

            default ->
                    throw new IllegalArgumentException(
                            "Invalid approval level"
                    );
        };
    }

    private String normalizeAction(
            String value
    ) {
        String action =
                value == null
                        ? ""
                        : value.trim()
                        .toUpperCase(Locale.ROOT);

        if (!Set.of(
                "APPROVE",
                "REJECT"
        ).contains(action)) {
            throw new IllegalArgumentException(
                    "Invalid approval action"
            );
        }

        return action;
    }

    private String checkerLabel(
            String role
    ) {
        return switch (role) {
            case ROLE_LEVEL_1_CHECKER ->
                    "Level 1 Checker";

            case ROLE_LEVEL_2_CHECKER ->
                    "Level 2 Checker";

            default ->
                    throw new IllegalArgumentException(
                            "Invalid checker role"
                    );
        };
    }

    private void validateRequiredText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clean(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}