/* Integrity and permission additions. This migration never removes business rows. */

IF COL_LENGTH('dbo.Designation','HierarchyOrder') IS NULL
    ALTER TABLE dbo.Designation ADD HierarchyOrder INT NOT NULL CONSTRAINT df_designation_hierarchy DEFAULT (100) WITH VALUES;
GO

;WITH ranked AS (
    SELECT DesignationId, ROW_NUMBER() OVER (ORDER BY DesignationId) AS rn
    FROM dbo.Designation
)
UPDATE d SET HierarchyOrder = r.rn * 10
FROM dbo.Designation d JOIN ranked r ON r.DesignationId=d.DesignationId
WHERE d.HierarchyOrder=100;

/* Batch-created employees are intentionally allowed to have incomplete media. */
ALTER TABLE dbo.employees ALTER COLUMN photo_path VARCHAR(500) NULL;
ALTER TABLE dbo.employees ALTER COLUMN signature_path VARCHAR(500) NULL;
ALTER TABLE dbo.employees ALTER COLUMN signature_valid_from DATE NULL;
ALTER TABLE dbo.employees ALTER COLUMN signature_valid_until DATE NULL;
/* The existing reference table uses BIGINT identifiers. */
ALTER TABLE dbo.employees ALTER COLUMN status_id BIGINT NULL;

IF COL_LENGTH('dbo.employee_requests','change_proposal_id') IS NULL
    ALTER TABLE dbo.employee_requests ADD change_proposal_id BIGINT NULL;
IF COL_LENGTH('dbo.employee_requests','status_id') IS NULL
    ALTER TABLE dbo.employee_requests ADD status_id BIGINT NULL;
IF COL_LENGTH('dbo.employee_requests','classification') IS NULL
    ALTER TABLE dbo.employee_requests ADD classification VARCHAR(10) NOT NULL CONSTRAINT df_requests_classification DEFAULT('BOTH') WITH VALUES;
IF COL_LENGTH('dbo.employee_requests','joining_date') IS NULL
    ALTER TABLE dbo.employee_requests ADD joining_date DATE NULL;

IF COL_LENGTH('dbo.import_batches','original_file_path') IS NULL
    ALTER TABLE dbo.import_batches ADD original_file_path VARCHAR(500) NULL;
IF COL_LENGTH('dbo.import_batches','dgm_comment') IS NULL
    ALTER TABLE dbo.import_batches ADD dgm_comment VARCHAR(500) NULL;
IF COL_LENGTH('dbo.import_batches','gm_comment') IS NULL
    ALTER TABLE dbo.import_batches ADD gm_comment VARCHAR(500) NULL;
IF COL_LENGTH('dbo.import_batches','rejection_reason') IS NULL
    ALTER TABLE dbo.import_batches ADD rejection_reason VARCHAR(500) NULL;
IF COL_LENGTH('dbo.import_batches','retry_of_batch_id') IS NULL
    ALTER TABLE dbo.import_batches ADD retry_of_batch_id BIGINT NULL;

IF COL_LENGTH('dbo.signature_books','file_sha256') IS NULL
    ALTER TABLE dbo.signature_books ADD file_sha256 VARCHAR(64) NULL;

/* SQL Server compiles column references for a whole batch.  Start a new batch
   after the additive ALTER statements so those columns can be referenced. */
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_employees_status')
    ALTER TABLE dbo.employees ADD CONSTRAINT fk_employees_status
        FOREIGN KEY(status_id) REFERENCES dbo.employee_status(status_id);

DECLARE @activeStatus INT=(SELECT TOP(1) status_id FROM dbo.employee_status WHERE status_name='Active' ORDER BY status_id);
UPDATE dbo.employees SET status_id=@activeStatus WHERE status_id IS NULL AND @activeStatus IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_request_change_proposal')
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_request_change_proposal
        FOREIGN KEY(change_proposal_id) REFERENCES dbo.employee_change_proposals(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_request_employee_status')
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_request_employee_status
        FOREIGN KEY(status_id) REFERENCES dbo.employee_status(status_id);
UPDATE dbo.employee_requests SET status_id=@activeStatus WHERE status_id IS NULL AND @activeStatus IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_batch_retry_parent')
    ALTER TABLE dbo.import_batches ADD CONSTRAINT fk_batch_retry_parent
        FOREIGN KEY(retry_of_batch_id) REFERENCES dbo.import_batches(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_employees_batch')
    ALTER TABLE dbo.employees ADD CONSTRAINT fk_employees_batch
        FOREIGN KEY(batch_id) REFERENCES dbo.import_batches(id);

IF OBJECT_ID('dbo.signature_book_entries','U') IS NULL
BEGIN
    CREATE TABLE dbo.signature_book_entries(
        id BIGINT IDENTITY PRIMARY KEY,
        book_id BIGINT NOT NULL,
        employee_id BIGINT NOT NULL,
        serial_number INT NOT NULL,
        signature_type VARCHAR(10) NOT NULL,
        signature_path VARCHAR(500) NOT NULL,
        CONSTRAINT uq_book_entry_serial UNIQUE(book_id,serial_number,signature_type),
        CONSTRAINT fk_book_entry_book FOREIGN KEY(book_id) REFERENCES dbo.signature_books(id),
        CONSTRAINT fk_book_entry_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id)
    );
END;

IF OBJECT_ID('dbo.tr_no_delete_signature_book_entries','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_signature_book_entries ON dbo.signature_book_entries INSTEAD OF DELETE AS BEGIN THROW 51010, ''Hard deletes are prohibited; deactivate the parent book instead'', 1; END');

/* An employee business identifier cannot be changed or reused. */
IF OBJECT_ID('dbo.tr_employees_immutable_number','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_employees_immutable_number ON dbo.employees AFTER UPDATE AS
BEGIN
 IF EXISTS(SELECT 1 FROM inserted i JOIN deleted d ON d.id=i.id WHERE i.employee_number<>d.employee_number)
 BEGIN ROLLBACK TRANSACTION; THROW 51011, ''Employee ID is immutable'', 1; END
END');

/* Baseline grants. Admin remains an application-level override, but grants are explicit and editable. */
IF COL_LENGTH('dbo.role_permissions','active') IS NULL
    ALTER TABLE dbo.role_permissions ADD active BIT NOT NULL CONSTRAINT df_role_permissions_active DEFAULT(1) WITH VALUES;
IF COL_LENGTH('dbo.role_permissions','deactivated_at') IS NULL
    ALTER TABLE dbo.role_permissions ADD deactivated_at DATETIME2 NULL;
GO

INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r CROSS JOIN dbo.permissions p
WHERE r.name='ADMIN' AND NOT EXISTS(SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r JOIN dbo.permissions p ON p.permission_key IN
 ('USER_PROPOSE','EMPLOYEE_VIEW','CONFIG_MANAGE','BATCH_UPLOAD','BOOK_GENERATE')
WHERE r.name='PD' AND NOT EXISTS(SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r JOIN dbo.permissions p ON p.permission_key IN
 ('APPROVE_DGM','EMPLOYEE_VIEW','EMPLOYEE_EDIT_PROPOSE')
WHERE r.name='DGM' AND NOT EXISTS(SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r JOIN dbo.permissions p ON p.permission_key IN
 ('APPROVE_GM','EMPLOYEE_VIEW','EMPLOYEE_EDIT_PROPOSE')
WHERE r.name='GM' AND NOT EXISTS(SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r JOIN dbo.permissions p ON p.permission_key IN ('EMPLOYEE_VIEW','AUDIT_VIEW')
WHERE r.name='BRANCH' AND NOT EXISTS(SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

UPDATE dbo.role_permissions SET active=1,deactivated_at=NULL
WHERE role_id IN (SELECT id FROM dbo.roles WHERE name='ADMIN');

IF DATABASE_PRINCIPAL_ID('sms_app') IS NOT NULL
BEGIN
    DENY DELETE ON SCHEMA::dbo TO sms_app;
    DENY UPDATE, DELETE ON dbo.audit_logs TO sms_app;
    DENY UPDATE, DELETE ON dbo.employee_versions TO sms_app;
END;
