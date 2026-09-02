package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Role;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccessControlServiceTest {
    @Test void localUserCannotViewForeignSignature() throws Exception {
        UserRepository users=mock(UserRepository.class); JdbcTemplate jdbc=mock(JdbcTemplate.class);
        User user=user("BRANCH","LOCAL"); when(users.findByUsername("branch")).thenReturn(Optional.of(user));
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any())).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(),eq(Integer.class),any(),any())).thenReturn(1);
        AccessControlService service=new AccessControlService(users,jdbc);
        assertTrue(service.canViewSignature("branch","LOCAL"));
        assertFalse(service.canViewSignature("branch","FOREIGN"));
    }

    @Test void explicitDenyOverridesRoleGrant() throws Exception {
        UserRepository users=mock(UserRepository.class); JdbcTemplate jdbc=mock(JdbcTemplate.class);
        when(users.findByUsername("branch")).thenReturn(Optional.of(user("BRANCH","BOTH")));
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any())).thenReturn(List.of(false));
        AccessControlService service=new AccessControlService(users,jdbc);
        assertFalse(service.has("branch","EMPLOYEE_VIEW"));
    }

    private User user(String roleName,String scope) throws Exception {
        Role role=new Role(roleName,roleName); role.setActive(true); setId(role,2L);
        User user=new User(); user.setUsername("branch"); user.setActive(true); user.setApprovalStatus("APPROVED"); user.setSignatureScope(scope); user.setRole(role); setId(user,3L); return user;
    }
    private void setId(Object value,Long id)throws Exception{Field f=value.getClass().getDeclaredField("id");f.setAccessible(true);f.set(value,id);}
}
