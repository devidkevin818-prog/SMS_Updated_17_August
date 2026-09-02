package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.List;

@Service("accessControl")
public class AccessControlService {
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public AccessControlService(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public boolean has(String username, String permissionKey) {
        if (username == null) return false;
        User user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isActive() || !"APPROVED".equals(user.getApprovalStatus()) || !user.getRole().isActive()) return false;
        if ("ADMIN".equals(user.getRole().getName())) return true;
        List<Boolean> override = jdbc.query("""
                SELECT up.allowed FROM user_permissions up
                JOIN permissions p ON p.id=up.permission_id AND p.active=1
                WHERE up.user_id=? AND up.active=1 AND p.permission_key=?
                """, (rs, row) -> rs.getBoolean(1), user.getId(), permissionKey);
        if (!override.isEmpty()) return override.getFirst();
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM role_permissions rp
                JOIN permissions p ON p.id=rp.permission_id AND p.active=1
                WHERE rp.role_id=? AND rp.active=1 AND p.permission_key=?
                """, Integer.class, user.getRole().getId(), permissionKey);
        return count != null && count > 0;
    }

    public void require(String username, String permissionKey) {
        if (!has(username, permissionKey)) throw new AccessDeniedException("Permission denied: " + permissionKey);
    }

    @Transactional(readOnly = true)
    public boolean canViewSignature(String username, String signatureType) {
        User user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isActive() || !"APPROVED".equals(user.getApprovalStatus()) || !user.getRole().isActive()) return false;
        if ("ADMIN".equals(user.getRole().getName())) return true;
        if (!has(username, "EMPLOYEE_VIEW")) return false;
        String requested = normalizeScope(signatureType);
        String scope = normalizeScope(user.getSignatureScope());
        return "BOTH".equals(scope) || scope.equals(requested);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyRole(String username, String... roles) {
        User user = users.findByUsername(username).orElse(null);
        if (user == null || !user.isActive() || !"APPROVED".equals(user.getApprovalStatus()) || user.getRole() == null || !user.getRole().isActive()) return false;
        return Set.of(roles).contains(user.getRole().getName());
    }

    @Transactional(readOnly = true)
    public User requireUser(String username) {
        return users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    @Transactional(readOnly = true)
    public String roleName(String username) {
        User user = users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("User not found"));
        if (!user.isActive() || user.getRole() == null || !user.getRole().isActive()) throw new AccessDeniedException("Inactive account");
        return user.getRole().getName();
    }

    private String normalizeScope(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("LOCAL") && !normalized.equals("FOREIGN") && !normalized.equals("BOTH")) {
            throw new IllegalArgumentException("Invalid signature scope");
        }
        return normalized;
    }
}
