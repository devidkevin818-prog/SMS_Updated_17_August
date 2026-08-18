package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.dto.UserUpdateForm;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;

    public List<Branch> getBranches() {
        return branchRepository.findAllByOrderByBranchNameAsc();
    }

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, BranchRepository branchRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public void createUser(UserForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();
        String branchId = form.getBranchId().trim();
        if (userRepository.existsByUsername(username)) throw new IllegalArgumentException("Username already exists");
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email already exists");
        Role role = roleRepository.findByName(form.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setBranchId(branchId);
        user.setRole(role);
        try {
            // Flush here so a database constraint error can be shown on the form
            // instead of surfacing after this method as a white error page.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Username or email already exists, or the user data is too long", exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(int page) { return userRepository.findAll(PageRequest.of(page, 20)); }

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
        if (userRepository.existsByEmailAndIdNot(email, id)) throw new IllegalArgumentException("Email already exists");
        if (form.getPassword() != null && !form.getPassword().isBlank() && form.getPassword().length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters");
        }
        Role role = roleRepository.findByName(form.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));
        user.setFullName(form.getFullName().trim());
        user.setBranchId(form.getBranchId().trim());
        user.setEmail(email);
        user.setRole(role);
        user.setActive(form.isActive());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
    }
}
