package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.AdminPasswordResetForm;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServicePasswordResetTest {
    private UserService service;
    private PasswordEncoder passwordEncoder;
    private User user;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(userRepository, mock(RoleRepository.class), passwordEncoder,
                mock(BranchRepository.class));
        user = new User();
        user.setPasswordHash("old-hash");
        user.setMustChangePassword(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void resetsPasswordAndRequiresChangeAtNextLogin() {
        AdminPasswordResetForm form = form("temporary-password", "temporary-password");
        when(passwordEncoder.matches("temporary-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("temporary-password")).thenReturn("temporary-hash");

        service.resetPassword(7L, form);

        assertEquals("temporary-hash", user.getPasswordHash());
        assertTrue(user.isMustChangePassword());
    }

    @Test
    void rejectsMismatchedConfirmationWithoutChangingPassword() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(7L, form("temporary-password", "different")));

        assertEquals("Temporary password and confirmation do not match", exception.getMessage());
        assertEquals("old-hash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    private AdminPasswordResetForm form(String password, String confirmation) {
        AdminPasswordResetForm form = new AdminPasswordResetForm();
        form.setNewPassword(password);
        form.setConfirmPassword(confirmation);
        return form;
    }
}
