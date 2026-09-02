/* Additive configuration, per-user permission overrides, and approval actors. */

IF OBJECT_ID('dbo.system_settings','U') IS NULL
BEGIN
    CREATE TABLE dbo.system_settings(
        id BIGINT IDENTITY PRIMARY KEY,
        setting_key VARCHAR(80) NOT NULL CONSTRAINT uq_system_settings_key UNIQUE,
        setting_value NVARCHAR(1000) NOT NULL,
        description NVARCHAR(300) NULL,
        active BIT NOT NULL CONSTRAINT df_system_settings_active DEFAULT(1),
        updated_by BIGINT NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT df_system_settings_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_system_settings_user FOREIGN KEY(updated_by) REFERENCES dbo.users(id)
    );
END;

IF NOT EXISTS(SELECT 1 FROM dbo.system_settings WHERE setting_key='EMPLOYEE_ID_REGEX')
    INSERT dbo.system_settings(setting_key,setting_value,description)
    VALUES('EMPLOYEE_ID_REGEX','^UB_PLC[0-9]{6}$','Regular expression applied to all newly entered employee IDs');

IF OBJECT_ID('dbo.user_permissions','U') IS NULL
BEGIN
    CREATE TABLE dbo.user_permissions(
        id BIGINT IDENTITY PRIMARY KEY,
        user_id BIGINT NOT NULL,
        permission_id BIGINT NOT NULL,
        allowed BIT NOT NULL,
        active BIT NOT NULL CONSTRAINT df_user_permissions_active DEFAULT(1),
        granted_by BIGINT NOT NULL,
        granted_at DATETIME2 NOT NULL CONSTRAINT df_user_permissions_granted DEFAULT SYSUTCDATETIME(),
        deactivated_at DATETIME2 NULL,
        CONSTRAINT uq_user_permissions UNIQUE(user_id,permission_id),
        CONSTRAINT fk_up_user FOREIGN KEY(user_id) REFERENCES dbo.users(id),
        CONSTRAINT fk_up_permission FOREIGN KEY(permission_id) REFERENCES dbo.permissions(id),
        CONSTRAINT fk_up_granter FOREIGN KEY(granted_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX ix_user_permissions_lookup ON dbo.user_permissions(user_id,permission_id,active);
END;

IF COL_LENGTH('dbo.user_creation_requests','dgm_decided_by') IS NULL
    ALTER TABLE dbo.user_creation_requests ADD dgm_decided_by BIGINT NULL;
IF COL_LENGTH('dbo.user_creation_requests','gm_decided_by') IS NULL
    ALTER TABLE dbo.user_creation_requests ADD gm_decided_by BIGINT NULL;
IF COL_LENGTH('dbo.user_creation_requests','dgm_decided_at') IS NULL
    ALTER TABLE dbo.user_creation_requests ADD dgm_decided_at DATETIME2 NULL;
IF COL_LENGTH('dbo.user_creation_requests','gm_decided_at') IS NULL
    ALTER TABLE dbo.user_creation_requests ADD gm_decided_at DATETIME2 NULL;

IF COL_LENGTH('dbo.import_batches','dgm_decided_by') IS NULL
    ALTER TABLE dbo.import_batches ADD dgm_decided_by BIGINT NULL;
IF COL_LENGTH('dbo.import_batches','gm_decided_by') IS NULL
    ALTER TABLE dbo.import_batches ADD gm_decided_by BIGINT NULL;
IF COL_LENGTH('dbo.import_batches','dgm_decided_at') IS NULL
    ALTER TABLE dbo.import_batches ADD dgm_decided_at DATETIME2 NULL;
IF COL_LENGTH('dbo.import_batches','gm_decided_at') IS NULL
    ALTER TABLE dbo.import_batches ADD gm_decided_at DATETIME2 NULL;
GO

IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_ucr_dgm_actor')
    ALTER TABLE dbo.user_creation_requests ADD CONSTRAINT fk_ucr_dgm_actor FOREIGN KEY(dgm_decided_by) REFERENCES dbo.users(id);
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_ucr_gm_actor')
    ALTER TABLE dbo.user_creation_requests ADD CONSTRAINT fk_ucr_gm_actor FOREIGN KEY(gm_decided_by) REFERENCES dbo.users(id);
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_batch_dgm_actor')
    ALTER TABLE dbo.import_batches ADD CONSTRAINT fk_batch_dgm_actor FOREIGN KEY(dgm_decided_by) REFERENCES dbo.users(id);
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_batch_gm_actor')
    ALTER TABLE dbo.import_batches ADD CONSTRAINT fk_batch_gm_actor FOREIGN KEY(gm_decided_by) REFERENCES dbo.users(id);
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_users_created_by')
    ALTER TABLE dbo.users ADD CONSTRAINT fk_users_created_by FOREIGN KEY(created_by) REFERENCES dbo.users(id);

IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.signature_book_access') AND name='ux_book_access_user')
    CREATE UNIQUE INDEX ux_book_access_user ON dbo.signature_book_access(book_id,user_id) WHERE user_id IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.signature_book_access') AND name='ux_book_access_role')
    CREATE UNIQUE INDEX ux_book_access_role ON dbo.signature_book_access(book_id,role_id) WHERE role_id IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.roles') AND name='ux_roles_active_hierarchy')
    CREATE UNIQUE INDEX ux_roles_active_hierarchy ON dbo.roles(hierarchy_order) WHERE active=1 AND hierarchy_order IS NOT NULL;

IF OBJECT_ID('dbo.tr_signature_book_entries_immutable','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_signature_book_entries_immutable ON dbo.signature_book_entries INSTEAD OF UPDATE AS BEGIN THROW 51012, ''Signature-book entries are immutable'', 1; END');

IF OBJECT_ID('dbo.tr_no_delete_system_settings','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_system_settings ON dbo.system_settings INSTEAD OF DELETE AS BEGIN THROW 51010, ''Hard deletes are prohibited; deactivate the setting instead'', 1; END');
IF OBJECT_ID('dbo.tr_no_delete_user_permissions','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_user_permissions ON dbo.user_permissions INSTEAD OF DELETE AS BEGIN THROW 51010, ''Hard deletes are prohibited; deactivate the override instead'', 1; END');

/* Create a least-privilege database role. Production application users should
   be added to this role instead of connecting as db_owner/sysadmin. */
IF DATABASE_PRINCIPAL_ID('sms_app') IS NULL
    CREATE ROLE sms_app AUTHORIZATION dbo;
DENY DELETE ON SCHEMA::dbo TO sms_app;
DENY UPDATE, DELETE ON dbo.audit_logs TO sms_app;
DENY UPDATE, DELETE ON dbo.employee_versions TO sms_app;
DENY UPDATE, DELETE ON dbo.signature_book_entries TO sms_app;

