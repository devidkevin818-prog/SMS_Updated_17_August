package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.PasswordChangeForm;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirstLoginServiceTest {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private FirstLoginService service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new FirstLoginService(userRepository, passwordEncoder, "");
        user = new User();
        user.setUsername("employee");
        user.setPasswordHash("old-hash");
        user.setMustChangePassword(true);
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(user));
    }

    @Test
    void changesPasswordAfterValidatingCurrentPasswordAndConfirmation() {
        PasswordChangeForm form = form("current-password", "new-password", "new-password");
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changePassword("employee", form);

        assertEquals("new-hash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        when(passwordEncoder.matches("incorrect", "old-hash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("employee", form("incorrect", "new-password", "new-password")));

        assertEquals("Current password is incorrect", exception.getMessage());
        assertEquals("old-hash", user.getPasswordHash());
    }

    @Test
    void rejectsMismatchedConfirmation() {
        when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("employee", form("current-password", "new-password", "different")));

        assertEquals("New password and confirmation do not match", exception.getMessage());
        assertEquals("old-hash", user.getPasswordHash());
    }

    private PasswordChangeForm form(String current, String password, String confirmation) {
        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword(current);
        form.setNewPassword(password);
        form.setConfirmPassword(confirmation);
        return form;
    }
}
