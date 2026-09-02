IF OBJECT_ID('dbo.signature_change_proposals','U') IS NULL
BEGIN
 CREATE TABLE dbo.signature_change_proposals(
   id BIGINT IDENTITY PRIMARY KEY, employee_id BIGINT NOT NULL, signature_type VARCHAR(10) NOT NULL,
   initiated_by BIGINT NOT NULL, initiator_remarks VARCHAR(500) NOT NULL,
   status VARCHAR(30) NOT NULL DEFAULT('PD_ACTION_REQUIRED'), created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
   submitted_version_id BIGINT NULL,
   CONSTRAINT ck_signature_change_type CHECK(signature_type IN ('LOCAL','FOREIGN')),
   CONSTRAINT fk_scp_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id),
   CONSTRAINT fk_scp_initiator FOREIGN KEY(initiated_by) REFERENCES dbo.users(id),
   CONSTRAINT fk_scp_version FOREIGN KEY(submitted_version_id) REFERENCES dbo.signature_versions(id));
 CREATE INDEX ix_scp_pd_queue ON dbo.signature_change_proposals(status,created_at);
 CREATE INDEX ix_scp_employee_type ON dbo.signature_change_proposals(employee_id,signature_type,status);
END;

IF COL_LENGTH('dbo.signature_versions','change_proposal_id') IS NULL
 ALTER TABLE dbo.signature_versions ADD change_proposal_id BIGINT NULL;
GO
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name='fk_sv_change_proposal')
 ALTER TABLE dbo.signature_versions ADD CONSTRAINT fk_sv_change_proposal FOREIGN KEY(change_proposal_id) REFERENCES dbo.signature_change_proposals(id);

IF OBJECT_ID('dbo.tr_no_delete_signature_change_proposals','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_signature_change_proposals ON dbo.signature_change_proposals INSTEAD OF DELETE AS BEGIN THROW 51010, ''Signature change proposals cannot be deleted'', 1; END');
