package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserServiceEmployeeNumberTest {
    @Test
    void rejectsCreationWhenEmployeeIdAlreadyBelongsToAUser() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(users, mock(RoleRepository.class),
                mock(PasswordEncoder.class), mock(BranchRepository.class));
        UserForm form = new UserForm();
        form.setUsername("new-user");
        form.setEmail("new-user@bank.local");
        form.setBranchId("1");
        form.setEmployeeNumber("123456");
        when(users.existsByEmployeeNumber("UB_PLC123456")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createUser(form));

        assertEquals("Employee ID already belongs to another user", exception.getMessage());
        verify(users, never()).saveAndFlush(any());
    }
}
