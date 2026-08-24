package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.dto.UserUpdateForm;
import com.bank.signaturemanagement.dto.AdminPasswordResetForm;
import com.bank.signaturemanagement.entity.Branch;
import com.bank.signaturemanagement.entity.Role;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;
    private final SecureTemporaryPasswordGenerator temporaryPasswordGenerator;

    public List<Branch> getBranches() {
        return branchRepository.findAllByOrderByBranchNameAsc();
    }

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, BranchRepository branchRepository) {
        this(userRepository, roleRepository, passwordEncoder, branchRepository,
                new SecureTemporaryPasswordGenerator());
    }

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, BranchRepository branchRepository,
                       SecureTemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    @Transactional
    public void createUser(UserForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();
        String branchId = form.getBranchId().trim();
        String employeeNumber = normalizeEmployeeNumber(form.getEmployeeNumber());
        if (userRepository.existsByUsername(username)) throw new IllegalArgumentException("Username already exists");
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email already exists");
        if (userRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new IllegalArgumentException("Employee ID already belongs to another user");
        }
        Role role = roleRepository.findByName(form.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName().trim());
        user.setEmployeeNumber(employeeNumber);
        user.setEmail(email);
        user.setBranchId(branchId);
        user.setRole(role);
        try {
            // Flush here so a database constraint error can be shown on the form
            // instead of surfacing after this method as a white error page.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Username, email, or employee ID already exists, or the user data is too long", exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(int page) { return userRepository.findAll(PageRequest.of(page, 20)); }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, String role, String branchId, Boolean active, int page) {
        int safePage = Math.max(page, 0);
        return userRepository.search(normalizeFilter(query), normalizeFilter(role),
                normalizeFilter(branchId), active,
                PageRequest.of(safePage, 20));
    }

    @Transactional(readOnly = true)
    public long getTotalUserCount() { return userRepository.count(); }

    @Transactional(readOnly = true)
    public long getActiveUserCount() { return userRepository.countByActiveTrue(); }

    @Transactional(readOnly = true)
    public long getInactiveUserCount() { return userRepository.countByActiveFalse(); }

    @Transactional(readOnly = true)
    public Map<String, String> getBranchNamesById() {
        return getBranches().stream().collect(Collectors.toMap(
                branch -> branch.getBranchId().toString(), Branch::getBranchName));
    }

    @Transactional(readOnly = true)
    public String getBranchName(String branchId) {
        if (branchId == null || branchId.isBlank()) return "Not assigned";
        try {
            return branchRepository.findById(Long.valueOf(branchId))
                    .map(Branch::getBranchName).orElse("Not assigned");
        } catch (NumberFormatException exception) {
            return "Not assigned";
        }
    }

    public String generateTemporaryPassword() {
        return temporaryPasswordGenerator.generate();
    }

    @Transactional
    public void recordSuccessfulLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLastLoginAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Set<String> getDuplicateEmployeeNumbers() {
        return userRepository.findDuplicateEmployeeNumbers().stream()
                .map(String::toUpperCase).collect(Collectors.toSet());
    }

    @Transactional
    public void toggleActive(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(!user.isActive());
    }

    @Transactional(readOnly = true)
    public List<Role> getRoles() { return roleRepository.findAll(); }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserUpdateForm getUpdateForm(Long id) {
        User user = getUser(id);
        UserUpdateForm form = new UserUpdateForm();
        form.setFullName(user.getFullName());
        form.setEmployeeNumber(EmployeeNumberFormat.editablePart(user.getEmployeeNumber()));
        form.setBranchId(user.getBranchId());
        form.setEmail(user.getEmail());
        form.setRoleName(user.getRole().getName());
        form.setActive(user.isActive());
        return form;
    }

    @Transactional
    public void updateUser(Long id, UserUpdateForm form) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String email = form.getEmail().trim();
        String employeeNumber = normalizeOptionalEmployeeNumber(form.getEmployeeNumber());
        if (userRepository.existsByEmailAndIdNot(email, id)) throw new IllegalArgumentException("Email already exists");
        if (employeeNumber != null && userRepository.existsByEmployeeNumberAndIdNot(employeeNumber, id)) {
            throw new IllegalArgumentException("Employee ID already belongs to another user");
        }
        if (form.getPassword() != null && !form.getPassword().isBlank() && form.getPassword().length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters");
        }
        Role role = roleRepository.findByName(form.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        user.setFullName(form.getFullName().trim());
        user.setEmployeeNumber(employeeNumber);
        user.setBranchId(form.getBranchId().trim());
        user.setEmail(email);
        user.setRole(role);
        user.setActive(form.isActive());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
    }

    @Transactional
    public void resetPassword(Long id, AdminPasswordResetForm form) {
        form.setResetMethod("manual");
        form.setRequirePasswordChange(true);
        resetPasswordSecure(id, form);
    }

    @Transactional
    public String resetPasswordSecure(Long id, AdminPasswordResetForm form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String password;
        if ("generate".equals(form.getResetMethod())) {
            password = form.getGeneratedPassword();
            if (password == null || password.isBlank()) password = temporaryPasswordGenerator.generate();
        } else if ("manual".equals(form.getResetMethod())) {
            password = form.getNewPassword();
            if (password == null || password.length() < 8) {
                throw new IllegalArgumentException("Password must contain at least 8 characters");
            }
            if (!password.equals(form.getConfirmPassword())) {
                throw new IllegalArgumentException("Temporary password and confirmation do not match");
            }
        } else {
            throw new IllegalArgumentException("Select a valid reset method");
        }
        if (password.length() < 8) throw new IllegalArgumentException("Password must contain at least 8 characters");
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Temporary password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setMustChangePassword(form.isRequirePasswordChange());
        return password;
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        return EmployeeNumberFormat.normalize(employeeNumber);
    }

    private String normalizeOptionalEmployeeNumber(String employeeNumber) {
        return employeeNumber == null || employeeNumber.isBlank()
                ? null : EmployeeNumberFormat.normalize(employeeNumber);
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }
}
