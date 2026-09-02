/* Dedicated read-only audit role. The account itself is created by InitialDataConfig
   so its BCrypt password can be supplied through configuration. */
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name='AUDIT')
    INSERT dbo.roles(name,description,active,hierarchy_order,system_role)
    VALUES('AUDIT','Read-only access to the complete immutable audit ledger',1,6,1);

IF EXISTS (SELECT 1 FROM dbo.roles WHERE name='AUDIT')
   AND EXISTS (SELECT 1 FROM dbo.permissions WHERE permission_key='AUDIT_VIEW')
   AND NOT EXISTS (
       SELECT 1 FROM dbo.role_permissions rp
       JOIN dbo.roles r ON r.id=rp.role_id
       JOIN dbo.permissions p ON p.id=rp.permission_id
       WHERE r.name='AUDIT' AND p.permission_key='AUDIT_VIEW')
    INSERT dbo.role_permissions(role_id,permission_id)
    SELECT r.id,p.id FROM dbo.roles r CROSS JOIN dbo.permissions p
    WHERE r.name='AUDIT' AND p.permission_key='AUDIT_VIEW';
