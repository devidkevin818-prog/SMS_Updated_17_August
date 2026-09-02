/* Additive platform migration. No existing table or business row is removed. */

IF COL_LENGTH('dbo.roles','hierarchy_order') IS NULL
    ALTER TABLE dbo.roles ADD hierarchy_order INT NULL;
IF COL_LENGTH('dbo.roles','system_role') IS NULL
    ALTER TABLE dbo.roles ADD system_role BIT NOT NULL CONSTRAINT df_roles_system DEFAULT (0) WITH VALUES;

/* SQL Server must compile references to newly added columns in a later batch. */
GO

UPDATE dbo.roles SET hierarchy_order = CASE name
    WHEN 'ADMIN' THEN 1 WHEN 'PD' THEN 2 WHEN 'DGM' THEN 3 WHEN 'GM' THEN 4 WHEN 'BRANCH' THEN 5
    ELSE COALESCE(hierarchy_order, 100) END
WHERE hierarchy_order IS NULL;
UPDATE dbo.roles SET system_role=1 WHERE name IN ('ADMIN','PD','DGM','GM','BRANCH');

IF COL_LENGTH('dbo.branches','active') IS NULL
    ALTER TABLE dbo.branches ADD active BIT NOT NULL CONSTRAINT df_branches_active DEFAULT (1) WITH VALUES;
IF COL_LENGTH('dbo.branches','description') IS NULL
    ALTER TABLE dbo.branches ADD description NVARCHAR(500) NULL;

IF OBJECT_ID('dbo.employee_status','U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_status(
        status_id INT IDENTITY PRIMARY KEY,
        status_name VARCHAR(100) NOT NULL CONSTRAINT uq_employee_status_name UNIQUE,
        active BIT NOT NULL CONSTRAINT df_employee_status_active DEFAULT (1),
        display_order INT NOT NULL CONSTRAINT df_employee_status_order DEFAULT (100)
    );
END;
IF COL_LENGTH('dbo.employee_status','active') IS NULL
    ALTER TABLE dbo.employee_status ADD active BIT NOT NULL CONSTRAINT df_employee_status_active2 DEFAULT (1) WITH VALUES;
IF COL_LENGTH('dbo.employee_status','display_order') IS NULL
    ALTER TABLE dbo.employee_status ADD display_order INT NOT NULL CONSTRAINT df_employee_status_order2 DEFAULT (100) WITH VALUES;

GO

IF NOT EXISTS(SELECT 1 FROM dbo.employee_status)
BEGIN
    INSERT dbo.employee_status(status_name,active,display_order) VALUES
      ('Active',1,10),('Transferred',1,20),('Resigned',1,30),('Promoted',1,40),
      ('On Leave',1,50),('Terminated',1,60);
END;

IF OBJECT_ID('dbo.permissions','U') IS NULL
BEGIN
    CREATE TABLE dbo.permissions(
        id BIGINT IDENTITY PRIMARY KEY, permission_key VARCHAR(80) NOT NULL CONSTRAINT uq_permissions_key UNIQUE,
        description VARCHAR(300) NULL, active BIT NOT NULL CONSTRAINT df_permissions_active DEFAULT(1));
END;
IF OBJECT_ID('dbo.role_permissions','U') IS NULL
BEGIN
    CREATE TABLE dbo.role_permissions(
        role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, granted_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT pk_role_permissions PRIMARY KEY(role_id,permission_id),
        CONSTRAINT fk_rp_role FOREIGN KEY(role_id) REFERENCES dbo.roles(id),
        CONSTRAINT fk_rp_permission FOREIGN KEY(permission_id) REFERENCES dbo.permissions(id));
END;

GO

MERGE dbo.permissions AS t USING (VALUES
 ('USER_MANAGE','Manage user access'),('USER_PROPOSE','Propose users'),
 ('APPROVE_DGM','DGM approval'),('APPROVE_GM','GM approval'),
 ('EMPLOYEE_VIEW','View employees'),('EMPLOYEE_EDIT_PROPOSE','Propose employee changes'),
 ('CONFIG_MANAGE','Manage reference data'),('AUDIT_VIEW','View audit ledger'),
 ('BATCH_UPLOAD','Upload employee batches'),('BOOK_GENERATE','Generate signature books'),
 ('BOOK_ACCESS_MANAGE','Manage signature book access')) s(permission_key,description)
ON t.permission_key=s.permission_key WHEN NOT MATCHED THEN INSERT(permission_key,description) VALUES(s.permission_key,s.description);

IF COL_LENGTH('dbo.users','signature_scope') IS NULL
    ALTER TABLE dbo.users ADD signature_scope VARCHAR(10) NOT NULL CONSTRAINT df_users_scope DEFAULT('BOTH') WITH VALUES;
IF COL_LENGTH('dbo.users','approval_status') IS NULL
    ALTER TABLE dbo.users ADD approval_status VARCHAR(30) NOT NULL CONSTRAINT df_users_approval DEFAULT('APPROVED') WITH VALUES;
IF COL_LENGTH('dbo.users','deactivated_at') IS NULL ALTER TABLE dbo.users ADD deactivated_at DATETIME2 NULL;
IF COL_LENGTH('dbo.users','created_by') IS NULL ALTER TABLE dbo.users ADD created_by BIGINT NULL;

IF COL_LENGTH('dbo.employees','locked') IS NULL
    ALTER TABLE dbo.employees ADD locked BIT NOT NULL CONSTRAINT df_employees_locked DEFAULT(0) WITH VALUES;
IF COL_LENGTH('dbo.employees','status_id') IS NULL ALTER TABLE dbo.employees ADD status_id INT NULL;
IF COL_LENGTH('dbo.employees','classification') IS NULL
    ALTER TABLE dbo.employees ADD classification VARCHAR(10) NOT NULL CONSTRAINT df_employees_class DEFAULT('BOTH') WITH VALUES;
IF COL_LENGTH('dbo.employees','joining_date') IS NULL ALTER TABLE dbo.employees ADD joining_date DATE NULL;
IF COL_LENGTH('dbo.employees','batch_id') IS NULL ALTER TABLE dbo.employees ADD batch_id BIGINT NULL;
IF COL_LENGTH('dbo.employees','active') IS NULL
    ALTER TABLE dbo.employees ADD active BIT NOT NULL CONSTRAINT df_employees_active DEFAULT(1) WITH VALUES;

IF OBJECT_ID('dbo.audit_logs','U') IS NULL
BEGIN
 CREATE TABLE dbo.audit_logs(
   id BIGINT IDENTITY PRIMARY KEY, user_id BIGINT NULL, username VARCHAR(50) NULL,
   action_type VARCHAR(80) NOT NULL, target_entity VARCHAR(80) NOT NULL, target_id VARCHAR(80) NULL,
   event_time DATETIME2(7) NOT NULL CONSTRAINT df_audit_time DEFAULT SYSUTCDATETIME(), ip_address VARCHAR(64) NULL,
   old_value NVARCHAR(MAX) NULL, new_value NVARCHAR(MAX) NULL, result VARCHAR(20) NOT NULL,
   correlation_id VARCHAR(80) NULL, details NVARCHAR(1000) NULL,
   CONSTRAINT fk_audit_user FOREIGN KEY(user_id) REFERENCES dbo.users(id));
 CREATE INDEX ix_audit_user_time ON dbo.audit_logs(user_id,event_time DESC);
 CREATE INDEX ix_audit_action_time ON dbo.audit_logs(action_type,event_time DESC);
 CREATE INDEX ix_audit_entity ON dbo.audit_logs(target_entity,target_id,event_time DESC);
END;

IF OBJECT_ID('dbo.tr_audit_logs_append_only','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_audit_logs_append_only ON dbo.audit_logs INSTEAD OF UPDATE, DELETE AS BEGIN THROW 51001, ''Audit logs are append-only'', 1; END');

IF OBJECT_ID('dbo.user_creation_requests','U') IS NULL
BEGIN
 CREATE TABLE dbo.user_creation_requests(
   id BIGINT IDENTITY PRIMARY KEY, proposed_by BIGINT NOT NULL, proposed_username VARCHAR(50) NOT NULL,
   proposed_password_hash VARCHAR(255) NOT NULL, proposed_full_name VARCHAR(100) NOT NULL,
   proposed_employee_number VARCHAR(30) NOT NULL, proposed_email VARCHAR(100) NOT NULL,
   proposed_branch_id VARCHAR(100) NOT NULL, proposed_role_id BIGINT NOT NULL,
   proposed_scope VARCHAR(10) NOT NULL, status VARCHAR(30) NOT NULL, rejection_reason VARCHAR(500) NULL,
   dgm_comment VARCHAR(500) NULL, gm_comment VARCHAR(500) NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
   decided_at DATETIME2 NULL, active BIT NOT NULL DEFAULT(1),
   CONSTRAINT fk_ucr_proposer FOREIGN KEY(proposed_by) REFERENCES dbo.users(id),
   CONSTRAINT fk_ucr_role FOREIGN KEY(proposed_role_id) REFERENCES dbo.roles(id));
 CREATE INDEX ix_ucr_status ON dbo.user_creation_requests(status,created_at);
END;

IF OBJECT_ID('dbo.employee_change_proposals','U') IS NULL
BEGIN
 CREATE TABLE dbo.employee_change_proposals(
   id BIGINT IDENTITY PRIMARY KEY, employee_id BIGINT NOT NULL, requested_by BIGINT NOT NULL,
   justification VARCHAR(500) NOT NULL, proposed_data NVARCHAR(MAX) NOT NULL, status VARCHAR(30) NOT NULL,
   pd_comment VARCHAR(500) NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), active BIT NOT NULL DEFAULT(1),
   CONSTRAINT fk_ecp_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id),
   CONSTRAINT fk_ecp_user FOREIGN KEY(requested_by) REFERENCES dbo.users(id));
END;

IF OBJECT_ID('dbo.employee_versions','U') IS NULL
BEGIN
 CREATE TABLE dbo.employee_versions(
   id BIGINT IDENTITY PRIMARY KEY, employee_id BIGINT NOT NULL, version_no INT NOT NULL,
   snapshot_json NVARCHAR(MAX) NOT NULL, changed_by BIGINT NULL, reason VARCHAR(500) NULL,
   created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
   CONSTRAINT uq_employee_version UNIQUE(employee_id,version_no),
   CONSTRAINT fk_ev_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id),
   CONSTRAINT fk_ev_user FOREIGN KEY(changed_by) REFERENCES dbo.users(id));
END;
IF OBJECT_ID('dbo.tr_employee_versions_append_only','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_employee_versions_append_only ON dbo.employee_versions INSTEAD OF UPDATE, DELETE AS BEGIN THROW 51002, ''Employee versions are append-only'', 1; END');

IF OBJECT_ID('dbo.import_batches','U') IS NULL
BEGIN
 CREATE TABLE dbo.import_batches(
   id BIGINT IDENTITY PRIMARY KEY, batch_number VARCHAR(40) NOT NULL UNIQUE, uploaded_by BIGINT NOT NULL,
   original_filename NVARCHAR(255) NOT NULL, status VARCHAR(30) NOT NULL, total_rows INT NOT NULL DEFAULT(0),
   succeeded_rows INT NOT NULL DEFAULT(0), failed_rows INT NOT NULL DEFAULT(0), uploaded_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
   active BIT NOT NULL DEFAULT(1), CONSTRAINT fk_batch_user FOREIGN KEY(uploaded_by) REFERENCES dbo.users(id));
END;
IF OBJECT_ID('dbo.import_batch_items','U') IS NULL
BEGIN
 CREATE TABLE dbo.import_batch_items(
   id BIGINT IDENTITY PRIMARY KEY, batch_id BIGINT NOT NULL, row_number INT NOT NULL, row_data NVARCHAR(MAX) NOT NULL,
   status VARCHAR(30) NOT NULL, error_detail NVARCHAR(1000) NULL, employee_id BIGINT NULL,
   CONSTRAINT uq_batch_row UNIQUE(batch_id,row_number), CONSTRAINT fk_item_batch FOREIGN KEY(batch_id) REFERENCES dbo.import_batches(id),
   CONSTRAINT fk_item_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id));
END;

IF OBJECT_ID('dbo.signature_books','U') IS NULL
BEGIN
 CREATE TABLE dbo.signature_books(
   id BIGINT IDENTITY PRIMARY KEY, book_number VARCHAR(40) NOT NULL UNIQUE, book_year INT NOT NULL,
   version_no INT NOT NULL, signature_type VARCHAR(10) NOT NULL, file_path VARCHAR(500) NOT NULL,
   generated_by BIGINT NOT NULL, generated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), active BIT NOT NULL DEFAULT(1),
   CONSTRAINT uq_book_version UNIQUE(book_year,signature_type,version_no),
   CONSTRAINT fk_book_user FOREIGN KEY(generated_by) REFERENCES dbo.users(id));
END;
IF OBJECT_ID('dbo.signature_book_access','U') IS NULL
BEGIN
 CREATE TABLE dbo.signature_book_access(
   id BIGINT IDENTITY PRIMARY KEY, book_id BIGINT NOT NULL, user_id BIGINT NULL, role_id BIGINT NULL,
   granted_by BIGINT NOT NULL, granted_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), active BIT NOT NULL DEFAULT(1),
   CONSTRAINT fk_ba_book FOREIGN KEY(book_id) REFERENCES dbo.signature_books(id),
   CONSTRAINT fk_ba_user FOREIGN KEY(user_id) REFERENCES dbo.users(id), CONSTRAINT fk_ba_role FOREIGN KEY(role_id) REFERENCES dbo.roles(id),
   CONSTRAINT fk_ba_granter FOREIGN KEY(granted_by) REFERENCES dbo.users(id),
   CONSTRAINT ck_ba_subject CHECK ((user_id IS NULL AND role_id IS NOT NULL) OR (user_id IS NOT NULL AND role_id IS NULL)));
END;

/* Block physical deletes at the database boundary for business tables. */
DECLARE @table sysname, @trigger sysname, @sql nvarchar(max);
DECLARE no_delete CURSOR LOCAL FAST_FORWARD FOR
SELECT name FROM sys.tables WHERE name IN
 ('users','employees','employee_requests','approval_history','employee_media_versions','roles','branches','Department','Designation',
  'employee_status','permissions','role_permissions','user_creation_requests','employee_change_proposals',
  'import_batches','import_batch_items','signature_books','signature_book_access');
OPEN no_delete; FETCH NEXT FROM no_delete INTO @table;
WHILE @@FETCH_STATUS=0 BEGIN
 SET @trigger='tr_no_delete_'+REPLACE(@table,' ','_');
 IF OBJECT_ID('dbo.'+@trigger,'TR') IS NULL BEGIN
   SET @sql='CREATE TRIGGER dbo.'+QUOTENAME(@trigger)+' ON dbo.'+QUOTENAME(@table)+
            ' INSTEAD OF DELETE AS BEGIN THROW 51010, ''Hard deletes are prohibited; deactivate the record instead'', 1; END';
   EXEC sp_executesql @sql;
 END;
 FETCH NEXT FROM no_delete INTO @table;
END;
CLOSE no_delete; DEALLOCATE no_delete;
