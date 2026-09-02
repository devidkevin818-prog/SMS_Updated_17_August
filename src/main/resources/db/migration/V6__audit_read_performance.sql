/* Keep newest-first audit pages fast as the append-only ledger grows. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.audit_logs') AND name='ix_audit_event_time')
    CREATE INDEX ix_audit_event_time ON dbo.audit_logs(event_time DESC);
