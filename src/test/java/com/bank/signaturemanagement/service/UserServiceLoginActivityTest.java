package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceLoginActivityTest {
    @Test
    void recordsSuccessfulLoginTimestamp() {
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        when(users.findByUsername("employee")).thenReturn(Optional.of(user));
        UserService service = new UserService(users, mock(RoleRepository.class),
                mock(PasswordEncoder.class), mock(BranchRepository.class));
        LocalDateTime before = LocalDateTime.now();

        service.recordSuccessfulLogin("employee");

        assertNotNull(user.getLastLoginAt());
        assertTrue(!user.getLastLoginAt().isBefore(before));
    }
}
