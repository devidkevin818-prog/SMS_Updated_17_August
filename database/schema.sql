/*
 Employee Signature Management System - Microsoft SQL Server schema

<<<<<<< HEAD
 Run this complete file in SQL Server Management Studio or IntelliJ's SQL console.
 It creates the database when necessary, creates the application tables, adds indexes,
 and inserts the application roles. Existing tables and roles are left unchanged.
=======
 PURPOSE
 -------
 This script creates or upgrades EmployeeSignatureDB without deleting existing
 business data. It is designed to be safe to run more than once.

 WHAT "SAFE TO RE-RUN" MEANS HERE
 ---------------------------------
 1. The database is created only when it does not already exist.
 2. Tables are created only when they do not already exist.
 3. Columns are added only when they do not already exist.
 4. Indexes and constraints are created only when missing.
 5. Seed/master data is inserted only when the value does not already exist.
 6. Existing non-NULL business values are not overwritten.
 7. No DROP TABLE, DELETE, TRUNCATE, or destructive reset is performed.

 IMPORTANT COMPATIBILITY NOTE
 ----------------------------
 The schema intentionally keeps legacy text columns such as designation,
 department, branch/branch_code and signature_path because the existing
 application may still use them. Lookup tables are retained alongside them.

 Run this complete file in SQL Server Management Studio (SSMS) or a SQL Server
 console that supports GO batch separators.
===============================================================================
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
*/

USE
master;
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
CREATE TABLE dbo.roles
(
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        VARCHAR(30) NOT NULL,
    description VARCHAR(200) NULL,
    active      BIT         NOT NULL CONSTRAINT df_roles_active DEFAULT (1),

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);
END;
GO

IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
CREATE TABLE dbo.users
(
    id                   BIGINT IDENTITY(1,1) NOT NULL,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    full_name            VARCHAR(100) NOT NULL,
    employee_number      VARCHAR(30)  NULL,
    email                VARCHAR(100) NOT NULL,
    role_id              BIGINT       NOT NULL,
<<<<<<< HEAD
    active               BIT          NOT NULL CONSTRAINT df_users_active DEFAULT (1),
    must_change_password BIT          NOT NULL CONSTRAINT df_users_must_change_password DEFAULT (1),
    created_at           DATETIME2    NOT NULL CONSTRAINT df_users_created_at DEFAULT (SYSDATETIME()),
=======
    active               BIT NOT NULL
        CONSTRAINT df_users_active DEFAULT (1),
    must_change_password BIT NOT NULL
        CONSTRAINT df_users_must_change_password DEFAULT (1),
    last_login_at        DATETIME2 NULL,
    branch_id            NVARCHAR(100) NULL,
    created_at           DATETIME2 NOT NULL
        CONSTRAINT df_users_created_at DEFAULT (SYSDATETIME()),
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles (id)
);
END;
GO

<<<<<<< HEAD
-- Upgrade databases created before first-login password changes were introduced.
IF COL_LENGTH('dbo.users', 'must_change_password') IS NULL
=======
/* Last successful authentication; null means the account has never logged in. */
IF COL_LENGTH(N'dbo.users', N'last_login_at') IS NULL
BEGIN
ALTER TABLE dbo.users ADD last_login_at DATETIME2 NULL;
END;
GO

/* Upgrade older users table: employee ID is optional for existing accounts. */
IF COL_LENGTH(N'dbo.users', N'employee_number') IS NULL
BEGIN
ALTER TABLE dbo.users ADD employee_number VARCHAR(30) NULL;
END;
GO

IF EXISTS (
    SELECT employee_number FROM dbo.users
    WHERE employee_number IS NOT NULL
    GROUP BY employee_number HAVING COUNT(*) > 1
)
THROW 51000, 'Duplicate users.employee_number values must be corrected before applying the unique index.', 1;
GO

IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'ix_users_employee_number'
      AND object_id = OBJECT_ID(N'dbo.users')
      AND is_unique = 0
)
DROP INDEX ix_users_employee_number ON dbo.users;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'ux_users_employee_number'
      AND object_id = OBJECT_ID(N'dbo.users')
)
CREATE UNIQUE INDEX ux_users_employee_number ON dbo.users (employee_number)
WHERE employee_number IS NOT NULL;
GO

/* Upgrade older users table: add first-login password flag when missing. */
IF COL_LENGTH(N'dbo.users', N'must_change_password') IS NULL
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
BEGIN
ALTER TABLE dbo.users
    ADD must_change_password BIT NOT NULL
    CONSTRAINT df_users_must_change_password DEFAULT (1) WITH VALUES;
END;
GO

<<<<<<< HEAD
IF OBJECT_ID('dbo.employee_requests', 'U') IS NULL
=======
/* Upgrade older users table: add branch_id only when missing. */
IF COL_LENGTH(N'dbo.users', N'branch_id') IS NULL
BEGIN
ALTER TABLE dbo.users ADD branch_id NVARCHAR(100) NULL;
END;
GO

/*
 Do NOT overwrite existing branch assignments.
 For older rows that have no branch, use Head Office's ID as text if available.
 This preserves the legacy NVARCHAR(100) column type from the existing project.
*/
DECLARE @HeadOfficeBranchId NVARCHAR(100);
SELECT @HeadOfficeBranchId = CONVERT(NVARCHAR(100), branch_id)
FROM dbo.branches
WHERE branch_name = N'Head Office';

IF @HeadOfficeBranchId IS NOT NULL
BEGIN
UPDATE dbo.users
SET branch_id = @HeadOfficeBranchId
WHERE branch_id IS NULL;
END;
GO

/*=============================================================================
  SECTION 4 - EMPLOYEES
=============================================================================*/

/*-----------------------------------------------------------------------------
  employees
  Stores the currently approved/live employee profile and signature information.

  employee_number         -> unique employee identifier
  photo_path              -> current employee photo
  signature_path          -> legacy/current primary signature path
  local_signature_path    -> local signature image
  foreign_signature_path  -> foreign signature image
  signature_valid_*       -> validity period of approved signature
  employee_status_id      -> Active / Inactive / Resign master status
  update_request_status   -> whether an update workflow is currently active
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.employees', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employees
(
    id                     BIGINT IDENTITY(1,1) NOT NULL,
    employee_number        VARCHAR(30)  NOT NULL,
    full_name              VARCHAR(100) NOT NULL,
    designation            VARCHAR(100) NOT NULL,
    department             VARCHAR(100) NOT NULL,
    branch_code            VARCHAR(100) NOT NULL,
    photo_path             VARCHAR(500) NOT NULL,
    signature_path         VARCHAR(500) NOT NULL,
    local_signature_path   VARCHAR(500) NULL,
    foreign_signature_path VARCHAR(500) NULL,
    signature_valid_from   DATE NULL,
    signature_valid_until  DATE NULL,
    employee_status_id     BIGINT NULL,
    update_request_status  BIT NOT NULL
        CONSTRAINT DF_employees_update_request_status DEFAULT (0),
    created_at             DATETIME2 NOT NULL
        CONSTRAINT df_employees_created_at DEFAULT (SYSDATETIME()),
    updated_at             DATETIME2 NOT NULL
        CONSTRAINT df_employees_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uq_employees_number UNIQUE (employee_number),
    CONSTRAINT fk_employees_status FOREIGN KEY (employee_status_id)
        REFERENCES dbo.employee_status (status_id)
);
END;
GO

/* Upgrade existing employees table with fields introduced later. */
IF COL_LENGTH(N'dbo.employees', N'signature_valid_from') IS NULL
ALTER TABLE dbo.employees ADD signature_valid_from DATE NULL;
GO

IF COL_LENGTH(N'dbo.employees', N'signature_valid_until') IS NULL
ALTER TABLE dbo.employees ADD signature_valid_until DATE NULL;
GO

IF COL_LENGTH(N'dbo.employees', N'update_request_status') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employees_update_request_status DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'local_signature_path') IS NULL
ALTER TABLE dbo.employees ADD local_signature_path VARCHAR(500) NULL;
GO

IF COL_LENGTH(N'dbo.employees', N'foreign_signature_path') IS NULL
ALTER TABLE dbo.employees ADD foreign_signature_path VARCHAR(500) NULL;
GO

IF COL_LENGTH(N'dbo.employees', N'employee_status_id') IS NULL
ALTER TABLE dbo.employees ADD employee_status_id BIGINT NULL;
GO

/* Add the employee-status FK only if it is missing and all values are valid. */
IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.employees')
      AND name = N'fk_employees_status'
)
AND NOT EXISTS
(
    SELECT 1
    FROM dbo.employees e
    LEFT JOIN dbo.employee_status s ON s.status_id = e.employee_status_id
    WHERE e.employee_status_id IS NOT NULL
      AND s.status_id IS NULL
)
BEGIN
ALTER TABLE dbo.employees WITH CHECK
    ADD CONSTRAINT fk_employees_status
    FOREIGN KEY (employee_status_id)
    REFERENCES dbo.employee_status (status_id);
END;
GO

/*
 Fill ONLY missing validity values. Existing validity dates remain untouched.
 This matches the previous project's intent while avoiding repeated overwrites.
*/
UPDATE dbo.employees
SET signature_valid_from = CAST(GETDATE() AS DATE)
WHERE signature_valid_from IS NULL;
GO

UPDATE dbo.employees
SET signature_valid_until = DATEFROMPARTS(YEAR(GETDATE()) + 1, 12, 31)
WHERE signature_valid_until IS NULL;
GO

/*=============================================================================
  SECTION 5 - EMPLOYEE REQUEST / APPROVAL WORKFLOW
=============================================================================*/

/*-----------------------------------------------------------------------------
  employee_requests
  Staging/workflow table for a new employee or an employee update submitted by
  PD and processed through DGM -> GM approval.

  requested_by             -> user who submitted the request
  target_employee_id       -> existing employee when this is an update request
  status                   -> PENDING_DGM / PENDING_GM / APPROVED / REJECTED
  updated_after_rejection  -> indicates resubmission/change after rejection
  update_request_status    -> distinguishes/flags an employee update request
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.employee_requests', N'U') IS NULL
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
BEGIN
CREATE TABLE dbo.employee_requests
(
    id             BIGINT IDENTITY(1,1) NOT NULL,
    requested_by   BIGINT       NOT NULL,
    employee_code  VARCHAR(30)  NOT NULL,
    employee_name  VARCHAR(100) NOT NULL,
    designation    VARCHAR(100) NOT NULL,
    department     VARCHAR(100) NOT NULL,
    branch         VARCHAR(100) NOT NULL,
    photo_path     VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    remark         VARCHAR(500) NOT NULL,
    requested_at   DATETIME2    NOT NULL CONSTRAINT df_requests_requested_at DEFAULT (SYSDATETIME()),
    completed_at   DATETIME2 NULL,

    CONSTRAINT pk_employee_requests PRIMARY KEY (id),
    CONSTRAINT fk_requests_user FOREIGN KEY (requested_by) REFERENCES dbo.users (id),
    CONSTRAINT ck_requests_status CHECK
        (status IN ('PENDING_DGM', 'PENDING_GM', 'APPROVED', 'REJECTED'))
);
END;
GO

IF OBJECT_ID('dbo.employees', 'U') IS NULL
BEGIN
CREATE TABLE dbo.employees
(
    id              BIGINT IDENTITY(1,1) NOT NULL,
    employee_number VARCHAR(30)  NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    designation     VARCHAR(100) NOT NULL,
    department      VARCHAR(100) NOT NULL,
    branch_code     VARCHAR(100) NOT NULL,
    photo_path      VARCHAR(500) NOT NULL,
    signature_path  VARCHAR(500) NOT NULL,
    created_at      DATETIME2    NOT NULL CONSTRAINT df_employees_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2    NOT NULL CONSTRAINT df_employees_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uq_employees_number UNIQUE (employee_number)
);
END;
GO

IF COL_LENGTH('dbo.employee_requests', 'target_employee_id') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD target_employee_id BIGINT NULL;
ALTER TABLE dbo.employee_requests
    ADD CONSTRAINT fk_requests_target_employee
        FOREIGN KEY (target_employee_id) REFERENCES dbo.employees (id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_target_employee')
CREATE INDEX ix_requests_target_employee ON dbo.employee_requests (target_employee_id, status);
GO

IF OBJECT_ID('dbo.approval_history', 'U') IS NULL
BEGIN
CREATE TABLE dbo.approval_history
(
    id             BIGINT IDENTITY(1,1) NOT NULL,
    request_id     BIGINT       NOT NULL,
    acted_by       BIGINT       NOT NULL,
    approval_level VARCHAR(10)  NOT NULL,
    action         VARCHAR(20)  NOT NULL,
    remark         VARCHAR(500) NOT NULL,
    action_at      DATETIME2    NOT NULL CONSTRAINT df_approval_action_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_approval_history PRIMARY KEY (id),
    CONSTRAINT fk_approval_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests (id),
    CONSTRAINT fk_approval_user FOREIGN KEY (acted_by) REFERENCES dbo.users (id),
    CONSTRAINT ck_approval_level CHECK (approval_level IN ('DGM', 'GM')),
    CONSTRAINT ck_approval_action CHECK (action IN ('APPROVED', 'REJECTED')
)
    );
END;
GO

IF OBJECT_ID('dbo.employee_media_versions', 'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_media_versions
(
    id             BIGINT IDENTITY(1,1) NOT NULL,
    employee_id    BIGINT       NOT NULL,
    request_id     BIGINT NULL,
    version_number INT          NOT NULL,
    photo_path     VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,
    approved_at    DATETIME2    NOT NULL CONSTRAINT df_media_versions_approved_at DEFAULT (SYSDATETIME()),
    CONSTRAINT pk_employee_media_versions PRIMARY KEY (id),
    CONSTRAINT fk_media_versions_employee FOREIGN KEY (employee_id) REFERENCES dbo.employees (id),
    CONSTRAINT fk_media_versions_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests (id),
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
    ON dbo.employee_media_versions (request_id) WHERE request_id IS NOT NULL;
GO

INSERT INTO dbo.employee_media_versions (employee_id, request_id, version_number, photo_path, signature_path, approved_at)
SELECT e.id, NULL, 1, e.photo_path, e.signature_path, e.updated_at
FROM dbo.employees e
WHERE NOT EXISTS (SELECT 1 FROM dbo.employee_media_versions v WHERE v.employee_id = e.id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_status_date')
CREATE INDEX ix_requests_status_date ON dbo.employee_requests (status, requested_at);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_requests_code')
CREATE INDEX ix_requests_code ON dbo.employee_requests (employee_code);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_employees_name')
CREATE INDEX ix_employees_name ON dbo.employees (full_name);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_approval_request')
CREATE INDEX ix_approval_request ON dbo.approval_history (request_id, action_at);
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

SELECT name, description, active
FROM dbo.roles
ORDER BY id;
GO

/*=======================For adding branch=========================*/
CREATE TABLE dbo.branches
(
    branch_id   BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_name NVARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO dbo.branches (branch_name)
VALUES (N'Head Office'),
       (N'Dhaka Branch'),
       (N'Chittagong Branch'),
       (N'Sylhet Branch'),
       (N'Rajshahi Branch'),
       (N'Khulna Branch');

<<<<<<< HEAD
ALTER TABLE dbo.users
    ADD branch_id NVARCHAR(100) NULL;

UPDATE dbo.users
SET branch_id = N'1'
WHERE branch_id IS NULL;

ALTER TABLE dbo.users
ALTER
COLUMN branch_id NVARCHAR(100) NOT NULL;


 /*     ============         for adding Signature Validation time ======*/
      USE
EmployeeSignatureDB;
GO

ALTER TABLE employee_requests
    ADD
        signature_valid_from DATE NULL,
    signature_valid_until DATE NULL;
GO

UPDATE employee_requests
SET signature_valid_from  = '2026-08-16',
    signature_valid_until = '2027-08-16'
WHERE signature_valid_from IS NULL
   OR signature_valid_until IS NULL;
GO

ALTER TABLE employee_requests
ALTER
COLUMN signature_valid_from DATE NOT NULL;
GO

ALTER TABLE employee_requests
ALTER
COLUMN signature_valid_until DATE NOT NULL;
GO

ALTER TABLE dbo.employees
    ADD signature_valid_from DATE NULL;

ALTER TABLE dbo.employees
    ADD signature_valid_until DATE NULL;

UPDATE dbo.employees
SET signature_valid_from  = CAST(GETDATE() AS DATE),
    signature_valid_until = DATEFROMPARTS(YEAR(GETDATE()) + 1, 12, 31)
WHERE signature_valid_from IS NULL
   OR signature_valid_until IS NULL;
/* =====================================*/

ALTER TABLE [EmployeeSignatureDB].[dbo].[employee_requests]
    ADD [updated_after_rejection] BIT NOT NULL
    CONSTRAINT DF_employee_requests_updated_after_rejection DEFAULT (0);


/*==================Update of 18/08/2026====================*/

ALTER TABLE dbo.employee_requests
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employee_requests_update_request_status DEFAULT 0;

ALTER TABLE dbo.employees
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employees_update_request_status DEFAULT 0;


CREATE TABLE dbo.signature_types (
                                     signature_type_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                     signature_type_name VARCHAR(20) NOT NULL UNIQUE,
                                     active BIT NOT NULL DEFAULT 1
);

INSERT INTO dbo.signature_types (signature_type_name)
VALUES
    ('Local'),
    ('Foreign');

ALTER TABLE dbo.employee_requests
    ADD local_signature_path VARCHAR(500) NULL,
    foreign_signature_path VARCHAR(500) NULL;

ALTER TABLE dbo.employees
    ADD local_signature_path VARCHAR(500) NULL,
    foreign_signature_path VARCHAR(500) NULL;

CREATE TABLE dbo.employee_status (
     status_id BIGINT IDENTITY(1,1) PRIMARY KEY,
     status_name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO dbo.employee_status (status_name)
VALUES
    ('Active'),
    ('Inactive'),
    ('Resign');

ALTER TABLE dbo.employees
    ADD employee_status_id BIGINT NULL;

/*==================Update of 19/08/2026====================*/

USE [EmployeeSignatureDB];
GO

IF OBJECT_ID('[dbo].[Department]', 'U') IS NULL
BEGIN
CREATE TABLE [dbo].[Department]
(
    [DepartmentId] BIGINT IDENTITY(1,1) NOT NULL,
    [DepartmentName] VARCHAR(200) NOT NULL,
    [Description] VARCHAR(500) NULL,
    [IsActive] BIT NOT NULL CONSTRAINT [DF_Department_IsActive] DEFAULT (1),
    [CreatedAt] DATETIME2 NOT NULL CONSTRAINT [DF_Department_CreatedAt] DEFAULT (GETDATE()),

    CONSTRAINT [PK_Department]
    PRIMARY KEY ([DepartmentId])
    );
END;
GO

USE [EmployeeSignatureDB];
GO

IF OBJECT_ID('[dbo].[Designation]', 'U') IS NULL
BEGIN
CREATE TABLE [dbo].[Designation]
(
    [DesignationId] BIGINT IDENTITY(1,1) NOT NULL,
    [DesignationName] VARCHAR(200) NOT NULL,
    [Description] VARCHAR(500) NULL,
    [IsActive] BIT NOT NULL CONSTRAINT [DF_Designation_IsActive] DEFAULT (1),
    [CreatedAt] DATETIME2 NOT NULL CONSTRAINT [DF_Designation_CreatedAt] DEFAULT (GETDATE()),

    CONSTRAINT [PK_Designation]
    PRIMARY KEY ([DesignationId])
    );
END;
GO

 INSERT INTO [EmployeeSignatureDB].[dbo].[Department]
    ([DepartmentName], [Description], [IsActive], [CreatedAt])
VALUES
    ('Human Resources', 'Human Resources Department', 1, GETDATE()),
    ('Information Technology', 'Information Technology Department', 1, GETDATE()),
    ('Finance', 'Finance Department', 1, GETDATE()),
    ('Accounts', 'Accounts Department', 1, GETDATE()),
    ('Administration', 'Administration Department', 1, GETDATE()),
    ('Audit', 'Audit Department', 1, GETDATE()),
    ('Operations', 'Operations Department', 1, GETDATE()),
    ('Credit', 'Credit Department', 1, GETDATE()),
    ('General Banking', 'General Banking Department', 1, GETDATE());


INSERT INTO [EmployeeSignatureDB].[dbo].[Designation]
([DesignationName], [Description], [IsActive], [CreatedAt])
VALUES
    ('Managing Director', 'Managing Director', 1, GETDATE()),
    ('General Manager', 'General Manager', 1, GETDATE()),
    ('Deputy General Manager', 'Deputy General Manager', 1, GETDATE()),
    ('Senior Officer', 'Senior Officer', 1, GETDATE()),
    ('Officer', 'Officer', 1, GETDATE());
=======
SELECT signature_type_id, signature_type_name, active
FROM dbo.signature_types
ORDER BY signature_type_id;
GO

ALTER TABLE [dbo].[employee_media_versions]
    ADD [foreign_signature_path] NVARCHAR(500) NULL;
GO
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
