IF OBJECT_ID('dbo.employee_media_requests','U') IS NULL
BEGIN
 CREATE TABLE dbo.employee_media_requests(
  id BIGINT IDENTITY PRIMARY KEY, employee_id BIGINT NOT NULL, submitted_by BIGINT NOT NULL,
  photo_path VARCHAR(500) NULL, local_signature_path VARCHAR(500) NULL, foreign_signature_path VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL DEFAULT('PENDING_DGM'), submitted_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  dgm_approver BIGINT NULL,dgm_decided_at DATETIME2 NULL,dgm_remarks VARCHAR(500) NULL,
  gm_approver BIGINT NULL,gm_decided_at DATETIME2 NULL,gm_remarks VARCHAR(500) NULL,rejection_remarks VARCHAR(500) NULL,
  CONSTRAINT ck_media_request_has_file CHECK(photo_path IS NOT NULL OR local_signature_path IS NOT NULL OR foreign_signature_path IS NOT NULL),
  CONSTRAINT fk_emr_employee FOREIGN KEY(employee_id) REFERENCES dbo.employees(id),
  CONSTRAINT fk_emr_submitter FOREIGN KEY(submitted_by) REFERENCES dbo.users(id),
  CONSTRAINT fk_emr_dgm FOREIGN KEY(dgm_approver) REFERENCES dbo.users(id),CONSTRAINT fk_emr_gm FOREIGN KEY(gm_approver) REFERENCES dbo.users(id));
 CREATE INDEX ix_emr_status ON dbo.employee_media_requests(status,submitted_at);
END;
IF OBJECT_ID('dbo.tr_no_delete_employee_media_requests','TR') IS NULL
EXEC('CREATE TRIGGER dbo.tr_no_delete_employee_media_requests ON dbo.employee_media_requests INSTEAD OF DELETE AS BEGIN THROW 51010, ''Media requests cannot be deleted'', 1; END');
