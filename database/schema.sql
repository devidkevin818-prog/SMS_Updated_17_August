/*
===============================================================================
 Employee Signature Management System
 Safe / Re-runnable Microsoft SQL Server Schema
===============================================================================

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

 CHANGE LOG
 ----------
 - Added a type-correction step for dbo.users.branch_id. Earlier runs of this
   database (or Hibernate's own ddl-auto) may have created branch_id as
   BIGINT. The application entity expects NVARCHAR(100), and Hibernate's
   schema validator fails startup on the mismatch. The new step converts
   branch_id to NVARCHAR(100) only if it is not already that type, and only
   runs once existing data is safe to convert. No data is lost: BIGINT values
   convert cleanly to their text representation.

 Run this complete file in SQL Server Management Studio (SSMS) or a SQL Server
 console that supports GO batch separators.
===============================================================================
*/

/*=============================================================================
  SECTION 1 - CREATE DATABASE IF IT DOES NOT EXIST
=============================================================================*/
USE [master];
GO

IF DB_ID(N'EmployeeSignatureDB') IS NULL
BEGIN
    PRINT 'Creating database EmployeeSignatureDB...';
EXEC (N'CREATE DATABASE [EmployeeSignatureDB]');
END
ELSE
BEGIN
    PRINT 'Database EmployeeSignatureDB already exists. No database recreated.';
END;
GO

USE [EmployeeSignatureDB];
GO

/*=============================================================================
  SECTION 2 - MASTER / LOOKUP TABLES
=============================================================================*/

/*-----------------------------------------------------------------------------
  2.1 roles
  Stores application authorization roles.
  Examples: ADMIN, PD, DGM, GM, BRANCH.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
CREATE TABLE dbo.roles
(
    id          BIGINT IDENTITY(1,1) NOT NULL,
    name        VARCHAR(30)  NOT NULL,
    description VARCHAR(200) NULL,
    active      BIT NOT NULL
        CONSTRAINT df_roles_active DEFAULT (1),

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);
END;
GO

/*-----------------------------------------------------------------------------
  2.2 branches
  Stores branch / office names that can be assigned to users.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.branches', N'U') IS NULL
BEGIN
CREATE TABLE dbo.branches
(
    branch_id   BIGINT IDENTITY(1,1) NOT NULL,
    branch_name NVARCHAR(100) NOT NULL,

    CONSTRAINT pk_branches PRIMARY KEY (branch_id),
    CONSTRAINT uq_branches_name UNIQUE (branch_name)
);
END;
GO

/*-----------------------------------------------------------------------------
  2.3 signature_types
  Master list of supported signature categories.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.signature_types', N'U') IS NULL
BEGIN
CREATE TABLE dbo.signature_types
(
    signature_type_id   BIGINT IDENTITY(1,1) NOT NULL,
    signature_type_name VARCHAR(20) NOT NULL,
    active              BIT NOT NULL
        CONSTRAINT df_signature_types_active DEFAULT (1),

    CONSTRAINT pk_signature_types PRIMARY KEY (signature_type_id),
    CONSTRAINT uq_signature_types_name UNIQUE (signature_type_name)
);
END;
GO

/*-----------------------------------------------------------------------------
  2.4 employee_status
  Master list describing whether an employee is Active, Inactive or Resigned.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.employee_status', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_status
(
    status_id   BIGINT IDENTITY(1,1) NOT NULL,
    status_name VARCHAR(20) NOT NULL,

    CONSTRAINT pk_employee_status PRIMARY KEY (status_id),
    CONSTRAINT uq_employee_status_name UNIQUE (status_name)
);
END;
GO

/*-----------------------------------------------------------------------------
  2.5 Department
  Master list of organizational departments.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.Department', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Department
(
    DepartmentId   BIGINT IDENTITY(1,1) NOT NULL,
    DepartmentName VARCHAR(200) NOT NULL,
    Description    VARCHAR(500) NULL,
    IsActive       BIT NOT NULL
        CONSTRAINT DF_Department_IsActive DEFAULT (1),
    CreatedAt      DATETIME2 NOT NULL
        CONSTRAINT DF_Department_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Department PRIMARY KEY (DepartmentId)
);
END;
GO

/* Add a uniqueness rule only when existing data permits it. */
IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.Department')
      AND name = N'UX_Department_DepartmentName'
)
AND NOT EXISTS
(
    SELECT DepartmentName
    FROM dbo.Department
    GROUP BY DepartmentName
    HAVING COUNT(*) > 1
)
BEGIN
CREATE UNIQUE INDEX UX_Department_DepartmentName
    ON dbo.Department (DepartmentName);
END;
GO

/*-----------------------------------------------------------------------------
  2.6 Designation
  Master list of employee designations / job titles.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.Designation', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Designation
(
    DesignationId   BIGINT IDENTITY(1,1) NOT NULL,
    DesignationName VARCHAR(200) NOT NULL,
    Description     VARCHAR(500) NULL,
    IsActive        BIT NOT NULL
        CONSTRAINT DF_Designation_IsActive DEFAULT (1),
    CreatedAt       DATETIME2 NOT NULL
        CONSTRAINT DF_Designation_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Designation PRIMARY KEY (DesignationId)
);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.Designation')
      AND name = N'UX_Designation_DesignationName'
)
AND NOT EXISTS
(
    SELECT DesignationName
    FROM dbo.Designation
    GROUP BY DesignationName
    HAVING COUNT(*) > 1
)
BEGIN
CREATE UNIQUE INDEX UX_Designation_DesignationName
    ON dbo.Designation (DesignationName);
END;
GO

/*=============================================================================
  SECTION 3 - USERS
=============================================================================*/

/*-----------------------------------------------------------------------------
  users
  Login accounts for system users.

  role_id              -> authorization role
  branch_id            -> legacy-compatible branch identifier stored as text
  active               -> whether login is enabled
  must_change_password -> forces password change after first/admin reset login
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
CREATE TABLE dbo.users
(
    id                   BIGINT IDENTITY(1,1) NOT NULL,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    full_name            VARCHAR(100) NOT NULL,
    email                VARCHAR(100) NOT NULL,
    role_id              BIGINT       NOT NULL,
    active               BIT NOT NULL
        CONSTRAINT df_users_active DEFAULT (1),
    must_change_password BIT NOT NULL
        CONSTRAINT df_users_must_change_password DEFAULT (1),
    branch_id            NVARCHAR(100) NULL,
    created_at           DATETIME2 NOT NULL
        CONSTRAINT df_users_created_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id)
        REFERENCES dbo.roles (id)
);
END;
GO

/* Upgrade older users table: add first-login password flag when missing. */
IF COL_LENGTH(N'dbo.users', N'must_change_password') IS NULL
BEGIN
ALTER TABLE dbo.users
    ADD must_change_password BIT NOT NULL
    CONSTRAINT df_users_must_change_password DEFAULT (1) WITH VALUES;
END;
GO

/* Upgrade older users table: add branch_id only when missing. */
IF COL_LENGTH(N'dbo.users', N'branch_id') IS NULL
BEGIN
ALTER TABLE dbo.users ADD branch_id NVARCHAR(100) NULL;
END;
GO

/*
 FIX: Correct branch_id's data type if it exists but was created with the
 wrong type (e.g. BIGINT from an earlier schema version or from Hibernate's
 own ddl-auto). The application entity maps branch_id as a String, and
 Hibernate's schema validator will refuse to start the app if the actual
 column type does not match. Converting BIGINT -> NVARCHAR(100) is a safe,
 lossless implicit conversion, so any existing branch_id values are kept.

 This block must run BEFORE the "Do NOT overwrite existing branch
 assignments" step below, since that step also touches branch_id.
*/

/* Step A: drop any foreign key that depends on users.branch_id. A numeric
   FK to dbo.branches (branch_id BIGINT) cannot coexist with the text-based
   branch_id design this script (and the application entity) uses, so this
   constraint - if present - is a leftover that must go before the column
   can change type. */
IF EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.users')
      AND name = N'fk_users_branch'
)
BEGIN
    PRINT 'Dropping fk_users_branch so branch_id can change type...';
ALTER TABLE dbo.users DROP CONSTRAINT fk_users_branch;
END;
GO

/* Step B: also catch any other, differently-named FK that happens to target
   users.branch_id, so re-running this script stays robust even if the
   constraint was created under a different name. */
DECLARE @fk_name SYSNAME;
DECLARE fk_cursor CURSOR FOR
SELECT fk.name
FROM sys.foreign_keys fk
         JOIN sys.foreign_key_columns fkc
              ON fkc.constraint_object_id = fk.object_id
         JOIN sys.columns c
              ON c.object_id = fkc.parent_object_id
                  AND c.column_id = fkc.parent_column_id
WHERE fk.parent_object_id = OBJECT_ID(N'dbo.users')
  AND c.name = N'branch_id';

OPEN fk_cursor;
FETCH NEXT FROM fk_cursor INTO @fk_name;
WHILE @@FETCH_STATUS = 0
BEGIN
    PRINT 'Dropping foreign key ' + @fk_name + ' so branch_id can change type...';
EXEC (N'ALTER TABLE dbo.users DROP CONSTRAINT [' + @fk_name + N']');
FETCH NEXT FROM fk_cursor INTO @fk_name;
END;
CLOSE fk_cursor;
DEALLOCATE fk_cursor;
GO

/* Step C: now safe to convert the column type. */
IF EXISTS
(
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.users')
      AND c.name = N'branch_id'
      AND t.name <> N'nvarchar'
)
BEGIN
    PRINT 'Converting dbo.users.branch_id to NVARCHAR(100)...';
ALTER TABLE dbo.users
ALTER COLUMN branch_id NVARCHAR(100) NULL;
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
BEGIN
CREATE TABLE dbo.employee_requests
(
    id                       BIGINT IDENTITY(1,1) NOT NULL,
    requested_by             BIGINT       NOT NULL,
    target_employee_id       BIGINT NULL,
    employee_code            VARCHAR(30)  NOT NULL,
    employee_name            VARCHAR(100) NOT NULL,
    designation              VARCHAR(100) NOT NULL,
    department               VARCHAR(100) NOT NULL,
    branch                   VARCHAR(100) NOT NULL,
    photo_path               VARCHAR(500) NOT NULL,
    signature_path           VARCHAR(500) NOT NULL,
    local_signature_path     VARCHAR(500) NULL,
    foreign_signature_path   VARCHAR(500) NULL,
    signature_valid_from     DATE NOT NULL,
    signature_valid_until    DATE NOT NULL,
    status                   VARCHAR(30)  NOT NULL,
    remark                   VARCHAR(500) NOT NULL,
    updated_after_rejection  BIT NOT NULL
        CONSTRAINT DF_employee_requests_updated_after_rejection DEFAULT (0),
    update_request_status    BIT NOT NULL
        CONSTRAINT DF_employee_requests_update_request_status DEFAULT (0),
    requested_at             DATETIME2 NOT NULL
        CONSTRAINT df_requests_requested_at DEFAULT (SYSDATETIME()),
    completed_at             DATETIME2 NULL,

    CONSTRAINT pk_employee_requests PRIMARY KEY (id),
    CONSTRAINT fk_requests_user FOREIGN KEY (requested_by)
        REFERENCES dbo.users (id),
    CONSTRAINT fk_requests_target_employee FOREIGN KEY (target_employee_id)
        REFERENCES dbo.employees (id),
    CONSTRAINT ck_requests_status CHECK
        (status IN ('PENDING_DGM', 'PENDING_GM', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_requests_signature_dates CHECK
        (signature_valid_until >= signature_valid_from)
);
END;
GO

/* Upgrade older request table one column at a time. */
IF COL_LENGTH(N'dbo.employee_requests', N'target_employee_id') IS NULL
ALTER TABLE dbo.employee_requests ADD target_employee_id BIGINT NULL;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_from') IS NULL
ALTER TABLE dbo.employee_requests ADD signature_valid_from DATE NULL;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_until') IS NULL
ALTER TABLE dbo.employee_requests ADD signature_valid_until DATE NULL;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'updated_after_rejection') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD updated_after_rejection BIT NOT NULL
    CONSTRAINT DF_employee_requests_updated_after_rejection DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'update_request_status') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employee_requests_update_request_status DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'local_signature_path') IS NULL
ALTER TABLE dbo.employee_requests ADD local_signature_path VARCHAR(500) NULL;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'foreign_signature_path') IS NULL
ALTER TABLE dbo.employee_requests ADD foreign_signature_path VARCHAR(500) NULL;
GO

/*
 Backfill ONLY missing request validity dates.
 The fixed historical dates are kept to remain compatible with your previous
 migration; rows already containing dates are never changed.
*/
UPDATE dbo.employee_requests
SET signature_valid_from = CONVERT(DATE, '2026-08-16')
WHERE signature_valid_from IS NULL;
GO

UPDATE dbo.employee_requests
SET signature_valid_until = CONVERT(DATE, '2027-08-16')
WHERE signature_valid_until IS NULL;
GO

/* Make the two request validity columns NOT NULL only after every old row has
   been safely populated. */
IF EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'signature_valid_from'
      AND is_nullable = 1
)
BEGIN
ALTER TABLE dbo.employee_requests
ALTER COLUMN signature_valid_from DATE NOT NULL;
END;
GO

IF EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'signature_valid_until'
      AND is_nullable = 1
)
BEGIN
ALTER TABLE dbo.employee_requests
ALTER COLUMN signature_valid_until DATE NOT NULL;
END;
GO

/* Add target employee FK when missing and existing data is valid. */
IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'fk_requests_target_employee'
)
AND NOT EXISTS
(
    SELECT 1
    FROM dbo.employee_requests r
    LEFT JOIN dbo.employees e ON e.id = r.target_employee_id
    WHERE r.target_employee_id IS NOT NULL
      AND e.id IS NULL
)
BEGIN
ALTER TABLE dbo.employee_requests WITH CHECK
    ADD CONSTRAINT fk_requests_target_employee
    FOREIGN KEY (target_employee_id)
    REFERENCES dbo.employees (id);
END;
GO

/* Add signature-date validation only when existing rows satisfy it. */
IF NOT EXISTS
(
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'ck_requests_signature_dates'
)
AND NOT EXISTS
(
    SELECT 1
    FROM dbo.employee_requests
    WHERE signature_valid_until < signature_valid_from
)
BEGIN
ALTER TABLE dbo.employee_requests WITH CHECK
    ADD CONSTRAINT ck_requests_signature_dates
    CHECK (signature_valid_until >= signature_valid_from);
END;
GO

/*-----------------------------------------------------------------------------
  approval_history
  Permanent audit trail of DGM/GM decisions for every employee request.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.approval_history', N'U') IS NULL
BEGIN
CREATE TABLE dbo.approval_history
(
    id             BIGINT IDENTITY(1,1) NOT NULL,
    request_id     BIGINT       NOT NULL,
    acted_by       BIGINT       NOT NULL,
    approval_level VARCHAR(10)  NOT NULL,
    action         VARCHAR(20)  NOT NULL,
    remark         VARCHAR(500) NOT NULL,
    action_at      DATETIME2 NOT NULL
        CONSTRAINT df_approval_action_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_approval_history PRIMARY KEY (id),
    CONSTRAINT fk_approval_request FOREIGN KEY (request_id)
        REFERENCES dbo.employee_requests (id),
    CONSTRAINT fk_approval_user FOREIGN KEY (acted_by)
        REFERENCES dbo.users (id),
    CONSTRAINT ck_approval_level CHECK (approval_level IN ('DGM', 'GM')),
    CONSTRAINT ck_approval_action CHECK (action IN ('APPROVED', 'REJECTED'))
    );
END;
GO

/* Upgrade databases created before action_at existed. */
IF COL_LENGTH(N'dbo.approval_history', N'action_at') IS NULL
BEGIN
ALTER TABLE dbo.approval_history
    ADD action_at DATETIME2 NOT NULL
    CONSTRAINT df_approval_action_at DEFAULT (SYSDATETIME()) WITH VALUES;
END;
GO

/*=============================================================================
  SECTION 6 - EMPLOYEE MEDIA / SIGNATURE VERSION HISTORY
=============================================================================*/

/*-----------------------------------------------------------------------------
  employee_media_versions
  Keeps approved historical copies of employee photo/signature information.
  This allows old signatures to remain available instead of being deleted.
-----------------------------------------------------------------------------*/
IF OBJECT_ID(N'dbo.employee_media_versions', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_media_versions
(
    id             BIGINT IDENTITY(1,1) NOT NULL,
    employee_id    BIGINT       NOT NULL,
    request_id     BIGINT NULL,
    version_number INT          NOT NULL,
    photo_path     VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,
    approved_at    DATETIME2 NOT NULL
        CONSTRAINT df_media_versions_approved_at DEFAULT (SYSDATETIME()),

    CONSTRAINT pk_employee_media_versions PRIMARY KEY (id),
    CONSTRAINT fk_media_versions_employee FOREIGN KEY (employee_id)
        REFERENCES dbo.employees (id),
    CONSTRAINT fk_media_versions_request FOREIGN KEY (request_id)
        REFERENCES dbo.employee_requests (id),
    CONSTRAINT uq_employee_media_version UNIQUE (employee_id, version_number)
);
END;
GO

/*
 Older schema versions used a UNIQUE constraint on request_id. SQL Server allows
 only one NULL in that style of unique constraint. A filtered unique index is
 better because many historical rows may legitimately have request_id = NULL.
 This removes only the obsolete constraint, never the table or its data.
*/
IF EXISTS
(
    SELECT 1
    FROM sys.key_constraints
    WHERE name = N'uq_employee_media_request'
      AND parent_object_id = OBJECT_ID(N'dbo.employee_media_versions')
)
BEGIN
ALTER TABLE dbo.employee_media_versions
DROP CONSTRAINT uq_employee_media_request;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_media_versions')
      AND name = N'ux_employee_media_request_not_null'
)
AND NOT EXISTS
(
    SELECT request_id
    FROM dbo.employee_media_versions
    WHERE request_id IS NOT NULL
    GROUP BY request_id
    HAVING COUNT(*) > 1
)
BEGIN
CREATE UNIQUE INDEX ux_employee_media_request_not_null
    ON dbo.employee_media_versions (request_id)
    WHERE request_id IS NOT NULL;
END;
GO

/* Create initial version-history row only for employees with no history yet. */
INSERT INTO dbo.employee_media_versions
(
    employee_id,
    request_id,
    version_number,
    photo_path,
    signature_path,
    approved_at
)
SELECT
    e.id,
    NULL,
    1,
    e.photo_path,
    e.signature_path,
    e.updated_at
FROM dbo.employees e
WHERE NOT EXISTS
          (
              SELECT 1
              FROM dbo.employee_media_versions v
              WHERE v.employee_id = e.id
          );
GO

/*=============================================================================
  SECTION 7 - INDEXES
  Indexes improve common searches and approval-queue queries.
=============================================================================*/

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'ix_requests_target_employee'
)
BEGIN
CREATE INDEX ix_requests_target_employee
    ON dbo.employee_requests (target_employee_id, status);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'ix_requests_status_date'
)
BEGIN
CREATE INDEX ix_requests_status_date
    ON dbo.employee_requests (status, requested_at);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_requests')
      AND name = N'ix_requests_code'
)
BEGIN
CREATE INDEX ix_requests_code
    ON dbo.employee_requests (employee_code);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employees')
      AND name = N'ix_employees_name'
)
BEGIN
CREATE INDEX ix_employees_name
    ON dbo.employees (full_name);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.approval_history')
      AND name = N'ix_approval_request'
)
BEGIN
CREATE INDEX ix_approval_request
    ON dbo.approval_history (request_id, action_at);
END;
GO

/*=============================================================================
  SECTION 8 - SEED / MASTER DATA
  Each value is inserted only when it does not already exist.
=============================================================================*/

/* 8.1 Application roles */
INSERT INTO dbo.roles (name, description, active)
SELECT v.name, v.description, 1
FROM
    (
        VALUES
            ('ADMIN',  'Manages users'),
            ('PD',     'Creates employee requests'),
            ('DGM',    'Level 1 approver'),
            ('GM',     'Level 2 approver'),
            ('BRANCH', 'Views approved employees')
    ) v(name, description)
WHERE NOT EXISTS
          (
              SELECT 1 FROM dbo.roles r WHERE r.name = v.name
          );
GO

/* 8.2 Branches */
INSERT INTO dbo.branches (branch_name)
SELECT v.branch_name
FROM
    (
        VALUES
            (N'Head Office'),
            (N'Dhaka Branch'),
            (N'Chittagong Branch'),
            (N'Sylhet Branch'),
            (N'Rajshahi Branch'),
            (N'Khulna Branch')
    ) v(branch_name)
WHERE NOT EXISTS
          (
              SELECT 1 FROM dbo.branches b WHERE b.branch_name = v.branch_name
          );
GO

/* If Head Office was created during this run, fill only still-unassigned users. */
DECLARE @SeedHeadOfficeBranchId NVARCHAR(100);
SELECT @SeedHeadOfficeBranchId = CONVERT(NVARCHAR(100), branch_id)
FROM dbo.branches
WHERE branch_name = N'Head Office';

IF @SeedHeadOfficeBranchId IS NOT NULL
BEGIN
UPDATE dbo.users
SET branch_id = @SeedHeadOfficeBranchId
WHERE branch_id IS NULL;
END;
GO

/* Preserve the original design: branch_id becomes required after old NULL rows
   have received the Head Office value. */
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE branch_id IS NULL)
AND EXISTS
(
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.users')
      AND name = N'branch_id'
      AND is_nullable = 1
)
BEGIN
ALTER TABLE dbo.users ALTER COLUMN branch_id NVARCHAR(100) NOT NULL;
END;
GO

/* 8.3 Signature types */
INSERT INTO dbo.signature_types (signature_type_name, active)
SELECT v.signature_type_name, 1
FROM (VALUES ('Local'), ('Foreign')) v(signature_type_name)
WHERE NOT EXISTS
          (
              SELECT 1
              FROM dbo.signature_types s
              WHERE s.signature_type_name = v.signature_type_name
          );
GO

/* 8.4 Employee statuses */
INSERT INTO dbo.employee_status (status_name)
SELECT v.status_name
FROM (VALUES ('Active'), ('Inactive'), ('Resign')) v(status_name)
WHERE NOT EXISTS
          (
              SELECT 1
              FROM dbo.employee_status s
              WHERE s.status_name = v.status_name
          );
GO

/*
 Existing employees are deliberately NOT assigned a status automatically here.
 A NULL status is preserved rather than guessing that an older employee is Active.
*/
/* 8.5 Departments */
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive, CreatedAt)
SELECT
    v.DepartmentName,
    v.Description,
    1,
    SYSDATETIME()
FROM
    (
        VALUES
            ('Human Resources',        'Human Resources Department'),
            ('Information Technology','Information Technology Department'),
            ('Finance',                'Finance Department'),
            ('Accounts',               'Accounts Department'),
            ('Administration',         'Administration Department'),
            ('Audit',                  'Audit Department'),
            ('Operations',             'Operations Department'),
            ('Credit',                 'Credit Department'),
            ('General Banking',        'General Banking Department')
    ) v(DepartmentName, Description)
WHERE NOT EXISTS
          (
              SELECT 1
              FROM dbo.Department d
              WHERE d.DepartmentName = v.DepartmentName
          );
GO

/* 8.6 Designations */
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive, CreatedAt)
SELECT
    v.DesignationName,
    v.Description,
    1,
    SYSDATETIME()
FROM
    (
        VALUES
            ('Managing Director',       'Managing Director'),
            ('General Manager',         'General Manager'),
            ('Deputy General Manager',  'Deputy General Manager'),
            ('Senior Officer',          'Senior Officer'),
            ('Officer',                 'Officer')
    ) v(DesignationName, Description)
WHERE NOT EXISTS
          (
              SELECT 1
              FROM dbo.Designation d
              WHERE d.DesignationName = v.DesignationName
          );
GO

/*=============================================================================
  SECTION 9 - OPTIONAL VERIFICATION OUTPUT
  These SELECTs change nothing. They simply confirm the key master data.
=============================================================================*/
PRINT 'EmployeeSignatureDB schema check completed.';

SELECT id, name, description, active
FROM dbo.roles
ORDER BY id;

SELECT branch_id, branch_name
FROM dbo.branches
ORDER BY branch_id;

SELECT status_id, status_name
FROM dbo.employee_status
ORDER BY status_id;

SELECT signature_type_id, signature_type_name, active
FROM dbo.signature_types
ORDER BY signature_type_id;
GO