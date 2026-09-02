package com.bank.signaturemanagement.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PermissionAdminService {
    public record PermissionView(long id, String key, String description, boolean granted) {}
    public record UserPermissionView(long id, String key, String description, String overrideMode) {}
    public record RoleView(long id, String name, String description, int hierarchyOrder, boolean active) {}
    public record UserView(long id, String username, String fullName, String roleName, boolean active) {}

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final AccessControlService access;

    public PermissionAdminService(JdbcTemplate jdbc, AuditService audit, AccessControlService access) {
        this.jdbc = jdbc; this.audit = audit; this.access = access;
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return jdbc.query("SELECT id,name,description,hierarchy_order,active FROM roles ORDER BY hierarchy_order,id",
                (rs,n) -> new RoleView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getInt(4),rs.getBoolean(5)));
    }

    @Transactional(readOnly = true)
    public List<UserView> users() {
        return jdbc.query("""
                SELECT u.id,u.username,u.full_name,r.name,u.active
                FROM users u JOIN roles r ON r.id=u.role_id
                ORDER BY u.active DESC,u.username
                """, (rs,n) -> new UserView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBoolean(5)));
    }

    @Transactional(readOnly = true)
    public List<PermissionView> permissions(long roleId) {
        return jdbc.query("""
                SELECT p.id,p.permission_key,p.description,
                  CASE WHEN rp.role_id IS NULL OR rp.active=0 THEN CAST(0 AS BIT) ELSE CAST(1 AS BIT) END
                FROM permissions p LEFT JOIN role_permissions rp ON rp.permission_id=p.id AND rp.role_id=?
                WHERE p.active=1 ORDER BY p.permission_key
                """, (rs,n) -> new PermissionView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)), roleId);
    }

    @Transactional(readOnly = true)
    public List<UserPermissionView> userPermissions(long userId) {
        return jdbc.query("""
                SELECT p.id,p.permission_key,p.description,
                  CASE WHEN up.id IS NULL OR up.active=0 THEN 'INHERIT'
                       WHEN up.allowed=1 THEN 'ALLOW' ELSE 'DENY' END
                FROM permissions p LEFT JOIN user_permissions up ON up.permission_id=p.id AND up.user_id=?
                WHERE p.active=1 ORDER BY p.permission_key
                """, (rs,n) -> new UserPermissionView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4)), userId);
    }

    @Transactional
    public void setGrant(long roleId, long permissionId, boolean granted, String actor) {
        access.require(actor, "USER_MANAGE");
        String roleName = jdbc.queryForObject("SELECT name FROM roles WHERE id=?", String.class, roleId);
        if ("ADMIN".equals(roleName) && !granted) throw new IllegalArgumentException("System Admin permissions cannot be restricted");
        boolean old = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT CASE WHEN EXISTS(SELECT 1 FROM role_permissions WHERE role_id=? AND permission_id=? AND active=1)
                THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END
                """, Boolean.class, roleId, permissionId));
        if (granted) {
            jdbc.update("IF EXISTS(SELECT 1 FROM role_permissions WHERE role_id=? AND permission_id=?) UPDATE role_permissions SET active=1,deactivated_at=NULL WHERE role_id=? AND permission_id=? ELSE INSERT role_permissions(role_id,permission_id,active) VALUES(?,?,1)",
                    roleId,permissionId,roleId,permissionId,roleId,permissionId);
        } else {
            jdbc.update("UPDATE role_permissions SET active=0,deactivated_at=SYSUTCDATETIME() WHERE role_id=? AND permission_id=?",roleId,permissionId);
        }
        audit.record(actor,"PERMISSION_CHANGE","ROLE",String.valueOf(roleId),null,"SUCCESS",
                "permission="+permissionId+",granted="+old,"permission="+permissionId+",granted="+granted,null);
    }

    @Transactional
    public void setUserOverride(long userId, long permissionId, String requestedMode, String actor) {
        access.require(actor, "USER_MANAGE");
        String mode = requestedMode == null ? "" : requestedMode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("INHERIT","ALLOW","DENY").contains(mode)) throw new IllegalArgumentException("Invalid override mode");
        String role = jdbc.queryForObject("SELECT r.name FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",String.class,userId);
        if ("ADMIN".equals(role)) throw new IllegalArgumentException("System Admin always has full access");
        List<String> prior = jdbc.query("SELECT CASE WHEN active=0 THEN 'INHERIT' WHEN allowed=1 THEN 'ALLOW' ELSE 'DENY' END FROM user_permissions WHERE user_id=? AND permission_id=?",
                (rs,n)->rs.getString(1),userId,permissionId);
        String old = prior.isEmpty()?"INHERIT":prior.getFirst();
        if ("INHERIT".equals(mode)) {
            jdbc.update("UPDATE user_permissions SET active=0,deactivated_at=SYSUTCDATETIME() WHERE user_id=? AND permission_id=?",userId,permissionId);
        } else {
            boolean allowed="ALLOW".equals(mode);
            jdbc.update("""
                    IF EXISTS(SELECT 1 FROM user_permissions WHERE user_id=? AND permission_id=?)
                      UPDATE user_permissions SET allowed=?,active=1,deactivated_at=NULL,granted_by=(SELECT id FROM users WHERE username=?),granted_at=SYSUTCDATETIME() WHERE user_id=? AND permission_id=?
                    ELSE
                      INSERT user_permissions(user_id,permission_id,allowed,active,granted_by) VALUES(?,?,?,1,(SELECT id FROM users WHERE username=?))
                    """,userId,permissionId,allowed,actor,userId,permissionId,userId,permissionId,allowed,actor);
        }
        audit.record(actor,"USER_PERMISSION_OVERRIDE","USER",String.valueOf(userId),null,"SUCCESS",
                "permission="+permissionId+",mode="+old,"permission="+permissionId+",mode="+mode,null);
    }

    @Transactional
    public void updateHierarchy(long roleId, int order, String actor) {
        access.require(actor, "USER_MANAGE");
        if (order < 1) throw new IllegalArgumentException("Hierarchy order must be positive");
        Integer collisions=jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id<>? AND active=1 AND hierarchy_order=?",Integer.class,roleId,order);
        if (collisions!=null&&collisions>0) throw new IllegalArgumentException("Another active role already uses this hierarchy order");
        Integer old=jdbc.queryForObject("SELECT hierarchy_order FROM roles WHERE id=?",Integer.class,roleId);
        if (jdbc.update("UPDATE roles SET hierarchy_order=? WHERE id=?",order,roleId)==0) throw new IllegalArgumentException("Role not found");
        audit.record(actor,"ROLE_HIERARCHY_CHANGE","ROLE",String.valueOf(roleId),null,"SUCCESS",String.valueOf(old),String.valueOf(order),null);
    }
}
