/*
 Employee Signature Management System - Microsoft SQL Server schema

 Run this complete file in SQL Server Management Studio or IntelliJ's SQL console.
 It creates the database when necessary, creates the application tables, adds indexes,
 and inserts the application roles. Existing tables and roles are left unchanged.
*/

USE master;
GO

IF DB_ID('EmployeeSignatureDB') IS NULL
BEGIN
    EXEC ('CREATE DATABASE EmployeeSignatureDB');
END;
GO

USE EmployeeSignatureDB;
GO

IF OBJECT_ID('dbo.roles', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.roles (
        id          BIGINT IDENTITY(1,1) NOT NULL,
        name        VARCHAR(30) NOT NULL,
        description VARCHAR(200) NULL,
        active      BIT NOT NULL CONSTRAINT df_roles_active DEFAULT (1),

        CONSTRAINT pk_roles PRIMARY KEY (id),
        CONSTRAINT uq_roles_name UNIQUE (name)
    );
END;
GO

IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id            BIGINT IDENTITY(1,1) NOT NULL,
        username      VARCHAR(50) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        full_name     VARCHAR(100) NOT NULL,
        email         VARCHAR(100) NOT NULL,
        role_id       BIGINT NOT NULL,
        active        BIT NOT NULL CONSTRAINT df_users_active DEFAULT (1),
        must_change_password BIT NOT NULL CONSTRAINT df_users_must_change_password DEFAULT (1),
        created_at    DATETIME2 NOT NULL CONSTRAINT df_users_created_at DEFAULT (SYSDATETIME()),

        CONSTRAINT pk_users PRIMARY KEY (id),
        CONSTRAINT uq_users_username UNIQUE (username),
        CONSTRAINT uq_users_email UNIQUE (email),
        CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
    );
END;
GO

-- Upgrade databases created before first-login password changes were introduced.
IF COL_LENGTH('dbo.users', 'must_change_password') IS NULL
BEGIN
    ALTER TABLE dbo.users
        ADD must_change_password BIT NOT NULL
            CONSTRAINT df_users_must_change_password DEFAULT (1) WITH VALUES;
END;
GO

IF OBJECT_ID('dbo.employee_requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_requests (
        id             BIGINT IDENTITY(1,1) NOT NULL,
        requested_by   BIGINT NOT NULL,
        employee_code  VARCHAR(30) NOT NULL,
        employee_name  VARCHAR(100) NOT NULL,
        designation    VARCHAR(100) NOT NULL,
        department     VARCHAR(100) NOT NULL,
        branch         VARCHAR(100) NOT NULL,
        photo_path     VARCHAR(500) NOT NULL,
        signature_path VARCHAR(500) NOT NULL,
        status          VARCHAR(30) NOT NULL,
        remark          VARCHAR(500) NOT NULL,
        requested_at    DATETIME2 NOT NULL CONSTRAINT df_requests_requested_at DEFAULT (SYSDATETIME()),
        completed_at    DATETIME2 NULL,

        CONSTRAINT pk_employee_requests PRIMARY KEY (id),
        CONSTRAINT fk_requests_user FOREIGN KEY (requested_by) REFERENCES dbo.users(id),
        CONSTRAINT ck_requests_status CHECK
            (status IN ('PENDING_DGM', 'PENDING_GM', 'APPROVED', 'REJECTED'))
    );
END;
GO

IF OBJECT_ID('dbo.employees', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.employees (
        id             BIGINT IDENTITY(1,1) NOT NULL,
        employee_number VARCHAR(30) NOT NULL,
        full_name       VARCHAR(100) NOT NULL,
        designation     VARCHAR(100) NOT NULL,
        department      VARCHAR(100) NOT NULL,
        branch_code     VARCHAR(100) NOT NULL,
        photo_path      VARCHAR(500) NOT NULL,
        signature_path  VARCHAR(500) NOT NULL,
        created_at      DATETIME2 NOT NULL CONSTRAINT df_employees_created_at DEFAULT (SYSDATETIME()),
        updated_at      DATETIME2 NOT NULL CONSTRAINT df_employees_updated_at DEFAULT (SYSDATETIME()),

        CONSTRAINT pk_employees PRIMARY KEY (id),
        CONSTRAINT uq_employees_number UNIQUE (employee_number)
    );
END;
GO

IF COL_LENGTH('dbo.employee_requests', 'target_employee_id') IS NULL
BEGIN
    ALTER TABLE dbo.employee_requests ADD target_employee_id BIGINT NULL;
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_requests_target_employee
        FOREIGN KEY (target_employee_id) REFERENCES dbo.employees(id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_target_employee')
    CREATE INDEX ix_requests_target_employee ON dbo.employee_requests(target_employee_id, status);
GO

IF OBJECT_ID('dbo.approval_history', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.approval_history (
        id             BIGINT IDENTITY(1,1) NOT NULL,
        request_id     BIGINT NOT NULL,
        acted_by       BIGINT NOT NULL,
        approval_level VARCHAR(10) NOT NULL,
        action          VARCHAR(20) NOT NULL,
        remark          VARCHAR(500) NOT NULL,
        action_at       DATETIME2 NOT NULL CONSTRAINT df_approval_action_at DEFAULT (SYSDATETIME()),

        CONSTRAINT pk_approval_history PRIMARY KEY (id),
        CONSTRAINT fk_approval_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests(id),
        CONSTRAINT fk_approval_user FOREIGN KEY (acted_by) REFERENCES dbo.users(id),
        CONSTRAINT ck_approval_level CHECK (approval_level IN ('DGM', 'GM')),
        CONSTRAINT ck_approval_action CHECK (action IN ('APPROVED', 'REJECTED'))
    );
END;
GO

IF OBJECT_ID('dbo.employee_media_versions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_media_versions (
        id              BIGINT IDENTITY(1,1) NOT NULL,
        employee_id     BIGINT NOT NULL,
        request_id      BIGINT NULL,
        version_number  INT NOT NULL,
        photo_path      VARCHAR(500) NOT NULL,
        signature_path  VARCHAR(500) NOT NULL,
        approved_at     DATETIME2 NOT NULL CONSTRAINT df_media_versions_approved_at DEFAULT (SYSDATETIME()),
        CONSTRAINT pk_employee_media_versions PRIMARY KEY (id),
        CONSTRAINT fk_media_versions_employee FOREIGN KEY (employee_id) REFERENCES dbo.employees(id),
        CONSTRAINT fk_media_versions_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests(id),
        CONSTRAINT uq_employee_media_version UNIQUE (employee_id, version_number)
    );
END;
GO

-- SQL Server permits only one NULL in a normal UNIQUE constraint. Older versions
-- of this script created uq_employee_media_request, which prevents backfilling
-- multiple existing employees whose request_id is NULL. Replace it safely.
IF EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = 'uq_employee_media_request'
      AND parent_object_id = OBJECT_ID('dbo.employee_media_versions')
)
    ALTER TABLE dbo.employee_media_versions DROP CONSTRAINT uq_employee_media_request;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ux_employee_media_request_not_null'
      AND object_id = OBJECT_ID('dbo.employee_media_versions')
)
    CREATE UNIQUE INDEX ux_employee_media_request_not_null
        ON dbo.employee_media_versions(request_id)
        WHERE request_id IS NOT NULL;
GO

INSERT INTO dbo.employee_media_versions (employee_id, request_id, version_number, photo_path, signature_path, approved_at)
SELECT e.id, NULL, 1, e.photo_path, e.signature_path, e.updated_at
FROM dbo.employees e
WHERE NOT EXISTS (SELECT 1 FROM dbo.employee_media_versions v WHERE v.employee_id = e.id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_status_date')
    CREATE INDEX ix_requests_status_date ON dbo.employee_requests(status, requested_at);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_code')
    CREATE INDEX ix_requests_code ON dbo.employee_requests(employee_code);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_employees_name')
    CREATE INDEX ix_employees_name ON dbo.employees(full_name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_approval_request')
    CREATE INDEX ix_approval_request ON dbo.approval_history(request_id, action_at);
GO

MERGE dbo.roles AS target
USING (VALUES
    ('ADMIN',  'Manages users'),
    ('PD',     'Creates employee requests'),
    ('DGM',    'Level 1 approver'),
    ('GM',     'Level 2 approver'),
    ('BRANCH', 'Views approved employees')
) AS source(name, description)
ON target.name = source.name
WHEN NOT MATCHED THEN
    INSERT (name, description, active)
    VALUES (source.name, source.description, 1);
GO

SELECT name, description, active FROM dbo.roles ORDER BY id;
GO


/*=======================For adding branch=========================*/
CREATE TABLE dbo.branches (
                              branch_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              branch_name NVARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO dbo.branches (branch_name)
VALUES
    (N'Head Office'),
    (N'Dhaka Branch'),
    (N'Chittagong Branch'),
    (N'Sylhet Branch'),
    (N'Rajshahi Branch'),
    (N'Khulna Branch');

ALTER TABLE dbo.users
    ADD branch_id NVARCHAR(100) NULL;

UPDATE dbo.users
SET branch_id = N'1'
WHERE branch_id IS NULL;

ALTER TABLE dbo.users
ALTER COLUMN branch_id NVARCHAR(100) NOT NULL;


 /*     ============         for adding Signature Validation time ======*/
      USE EmployeeSignatureDB;
GO

ALTER TABLE employee_requests
    ADD
        signature_valid_from DATE NULL,
    signature_valid_until DATE NULL;
GO

UPDATE employee_requests
SET
    signature_valid_from = '2026-08-16',
    signature_valid_until = '2027-08-16'
WHERE
    signature_valid_from IS NULL
   OR signature_valid_until IS NULL;
GO

ALTER TABLE employee_requests
ALTER COLUMN signature_valid_from DATE NOT NULL;
GO

ALTER TABLE employee_requests
ALTER COLUMN signature_valid_until DATE NOT NULL;
GO

ALTER TABLE employees
    ADD signature_valid_from DATE NOT NULL,
    signature_valid_until DATE NOT NULL;

  ALTER TABLE users
ALTER COLUMN branch_id BIGINT;
/* =====================================*/
