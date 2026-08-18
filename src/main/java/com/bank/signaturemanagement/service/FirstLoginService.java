package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.PasswordChangeForm;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FirstLoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Set<String> exemptUsers;

    public FirstLoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             @Value("${app.first-login.exempt-users:}") String exemptUsers) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.exemptUsers = Arrays.stream(exemptUsers.split(","))
                .map(String::trim).filter(value -> !value.isEmpty())
                .map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public boolean mustChangePassword(String username) {
        if (exemptUsers.contains(username.toLowerCase())) return false;
        return userRepository.findByUsername(username).map(User::isMustChangePassword).orElse(false);
    }

    @Transactional
    public void changePassword(String username, PasswordChangeForm form) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        user.setMustChangePassword(false);
    }
}


