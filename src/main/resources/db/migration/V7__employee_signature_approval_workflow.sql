/* Employee/signature workflow additions. Additive only; no business rows are removed. */
ALTER TABLE dbo.employee_requests ALTER COLUMN photo_path VARCHAR(500) NULL;
ALTER TABLE dbo.employee_requests ALTER COLUMN signature_path VARCHAR(500) NULL;
ALTER TABLE dbo.employee_requests ALTER COLUMN signature_valid_from DATE NULL;
ALTER TABLE dbo.employee_requests ALTER COLUMN signature_valid_until DATE NULL;
ALTER TABLE dbo.employee_media_versions ALTER COLUMN photo_path VARCHAR(500) NULL;
ALTER TABLE dbo.employee_media_versions ALTER COLUMN signature_path VARCHAR(500) NULL;

IF OBJECT_ID('dbo.signature_upload_batches','U') IS NULL
BEGIN
 CREATE TABLE dbo.signature_upload_batches(
   id BIGINT IDENTITY PRIMARY KEY, batch_number VARCHAR(50) NOT NULL UNIQUE,
   submitted_by BIGINT NOT NULL, total_files INT NOT NULL DEFAULT(0), matched_files INT NOT NULL DEFAULT(0),
   invalid_files INT NOT NULL DEFAULT(0), status VARCHAR(30) NOT NULL DEFAULT('DRAFT'),
   created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
   CONSTRAINT fk_signature_batch_user FOREIGN KEY(submitted_by) REFERENCES dbo.users(id));
END;

IF OBJECT_ID('dbo.signature_versions','U') IS NULL
BEGIN
 CREATE TABLE dbo.signature_versions(
   id BIGINT IDENTITY PRIMARY KEY, employee_id BIGINT NULL, employee_number VARCHAR(30) NOT NULL,
   signature_type VARCHAR(10) NOT NULL, version_number INT NOT NULL, file_path VARCHAR(500) NOT NULL,
   status VARCHAR(30) NOT NULL, current_approved BIT NOT NULL DEFAULT(0), submitted_by BIGINT NOT NULL,
   submitted_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), batch_id BIGINT NULL,
   dgm_approver BIGINT NULL, dgm_decided_at DATETIME2 NULL, dgm_remarks VARCHAR(500) NULL,
   gm_approver BIGINT NULL, gm_decided_at DATETIME2 NULL, gm_remarks VARCHAR(500) NULL,
   rejection_remarks VARCHAR(500) NULL,
   CONSTRAINT ck_signature_type CHECK(signature_type IN ('LOCAL','FOREIGN')),
   CONSTRAINT uq_signature_version UNIQUE(employee_number,signature_type,version_number),
   CONSTRAINT fk_sv_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id),
   CONSTRAINT fk_sv_submitter FOREIGN KEY(submitted_by) REFERENCES dbo.users(id),
   CONSTRAINT fk_sv_dgm FOREIGN KEY(dgm_approver) REFERENCES dbo.users(id),
   CONSTRAINT fk_sv_gm FOREIGN KEY(gm_approver) REFERENCES dbo.users(id),
   CONSTRAINT fk_sv_batch FOREIGN KEY(batch_id) REFERENCES dbo.signature_upload_batches(id));
 CREATE INDEX ix_sv_pending ON dbo.signature_versions(status,submitted_at);
 CREATE INDEX ix_sv_history ON dbo.signature_versions(employee_number,signature_type,version_number DESC);
 CREATE UNIQUE INDEX uq_sv_current ON dbo.signature_versions(employee_number,signature_type)
   WHERE current_approved=1;
END;

IF COL_LENGTH('dbo.signature_books','status') IS NULL
 ALTER TABLE dbo.signature_books ADD status VARCHAR(20) NOT NULL CONSTRAINT df_signature_book_status DEFAULT('CURRENT') WITH VALUES;
GO

;WITH ranked AS (SELECT id, ROW_NUMBER() OVER(PARTITION BY signature_type ORDER BY generated_at DESC,id DESC) rn FROM dbo.signature_books WHERE active=1)
UPDATE b SET status=CASE WHEN r.rn=1 THEN 'CURRENT' ELSE 'ARCHIVED' END
FROM dbo.signature_books b JOIN ranked r ON r.id=b.id;

IF OBJECT_ID('dbo.tr_no_delete_signature_versions','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_signature_versions ON dbo.signature_versions INSTEAD OF DELETE AS BEGIN THROW 51010, ''Signature history is append-only'', 1; END');
