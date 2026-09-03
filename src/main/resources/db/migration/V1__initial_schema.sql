/*
 Employee Signature Management System - Microsoft SQL Server
 Flyway V1: foundational, idempotent schema for the configured database.
 The application provisions that database before Flyway starts.
*/

/* ---------- Master tables ---------- */

IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.roles
    (
        id          BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_roles PRIMARY KEY,
        name        VARCHAR(30) NOT NULL CONSTRAINT uq_roles_name UNIQUE,
        description VARCHAR(200) NULL,
        active      BIT NOT NULL CONSTRAINT df_roles_active DEFAULT (1)
    );
END;
GO

IF OBJECT_ID(N'dbo.branches', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.branches
    (
        branch_id   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_branches PRIMARY KEY,
        branch_name NVARCHAR(255) NOT NULL CONSTRAINT uq_branches_name UNIQUE
    );
END;
GO

IF OBJECT_ID(N'dbo.Department', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Department
    (
        DepartmentId   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_department PRIMARY KEY,
        DepartmentName VARCHAR(100) NOT NULL CONSTRAINT uq_department_name UNIQUE,
        Description    VARCHAR(500) NULL,
        IsActive       BIT NULL CONSTRAINT df_department_active DEFAULT (1),
        CreatedAt      DATETIME2 NULL CONSTRAINT df_department_created DEFAULT (SYSDATETIME())
    );
END;
GO

IF OBJECT_ID(N'dbo.Designation', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Designation
    (
        DesignationId   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_designation PRIMARY KEY,
        DesignationName VARCHAR(255) NOT NULL CONSTRAINT uq_designation_name UNIQUE,
        Description     VARCHAR(500) NULL,
        IsActive        BIT NULL CONSTRAINT df_designation_active DEFAULT (1),
        CreatedAt       DATETIME2 NULL CONSTRAINT df_designation_created DEFAULT (SYSDATETIME())
    );
END;
GO

/* ---------- Application users ---------- */

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users
    (
        id                   BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_users PRIMARY KEY,
        username             VARCHAR(50) NOT NULL CONSTRAINT uq_users_username UNIQUE,
        password_hash        VARCHAR(255) NOT NULL,
        full_name            VARCHAR(100) NOT NULL,
        employee_number      VARCHAR(30) NULL,
        email                VARCHAR(100) NOT NULL CONSTRAINT uq_users_email UNIQUE,
        branch_id            VARCHAR(100) NOT NULL,
        role_id              BIGINT NOT NULL,
        active               BIT NOT NULL CONSTRAINT df_users_active DEFAULT (1),
        must_change_password BIT NOT NULL CONSTRAINT df_users_password_change DEFAULT (1),
        last_login_at        DATETIME2 NULL,
        created_at           DATETIME2 NOT NULL CONSTRAINT df_users_created DEFAULT (SYSDATETIME()),
        CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
    );
END;
GO

IF COL_LENGTH(N'dbo.users', N'employee_number') IS NULL
    ALTER TABLE dbo.users ADD employee_number VARCHAR(30) NULL;
GO
IF COL_LENGTH(N'dbo.users', N'branch_id') IS NULL
    ALTER TABLE dbo.users ADD branch_id VARCHAR(100) NULL;
GO
IF COL_LENGTH(N'dbo.users', N'must_change_password') IS NULL
    ALTER TABLE dbo.users ADD must_change_password BIT NOT NULL
        CONSTRAINT df_users_password_change DEFAULT (1) WITH VALUES;
GO
IF COL_LENGTH(N'dbo.users', N'last_login_at') IS NULL
    ALTER TABLE dbo.users ADD last_login_at DATETIME2 NULL;
GO

IF EXISTS
(
    SELECT employee_number
    FROM dbo.users
    WHERE employee_number IS NOT NULL
    GROUP BY employee_number
    HAVING COUNT(*) > 1
)
    THROW 51000, 'Duplicate users.employee_number values must be corrected before creating the unique index.', 1;
GO

IF EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.users')
      AND name = N'ix_users_employee_number'
      AND is_unique = 0
)
    DROP INDEX ix_users_employee_number ON dbo.users;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.users')
      AND name = N'ux_users_employee_number'
)
    CREATE UNIQUE INDEX ux_users_employee_number
        ON dbo.users(employee_number) WHERE employee_number IS NOT NULL;
GO

/* ---------- Approved employees ---------- */

IF OBJECT_ID(N'dbo.employees', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employees
    (
        id                     BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_employees PRIMARY KEY,
        employee_number        VARCHAR(30) NOT NULL CONSTRAINT uq_employees_number UNIQUE,
        full_name              VARCHAR(100) NOT NULL,
        designation            BIGINT NOT NULL,
        department             BIGINT NOT NULL,
        branch_code            BIGINT NOT NULL,
        photo_path             VARCHAR(500) NOT NULL,
        signature_path         VARCHAR(500) NOT NULL,
        foreign_signature_path VARCHAR(500) NULL,
        signature_valid_from   DATE NOT NULL,
        signature_valid_until  DATE NOT NULL,
        update_request_status  BIT NULL CONSTRAINT df_employees_update_request DEFAULT (0),
        created_at             DATETIME2 NOT NULL CONSTRAINT df_employees_created DEFAULT (SYSDATETIME()),
        updated_at             DATETIME2 NOT NULL CONSTRAINT df_employees_updated DEFAULT (SYSDATETIME()),
        CONSTRAINT fk_employees_designation FOREIGN KEY (designation)
            REFERENCES dbo.Designation(DesignationId),
        CONSTRAINT fk_employees_department FOREIGN KEY (department)
            REFERENCES dbo.Department(DepartmentId),
        CONSTRAINT fk_employees_branch FOREIGN KEY (branch_code)
            REFERENCES dbo.branches(branch_id)
    );
END;
GO

IF COL_LENGTH(N'dbo.employees', N'foreign_signature_path') IS NULL
    ALTER TABLE dbo.employees ADD foreign_signature_path VARCHAR(500) NULL;
GO
IF COL_LENGTH(N'dbo.employees', N'signature_valid_from') IS NULL
    ALTER TABLE dbo.employees ADD signature_valid_from DATE NULL;
GO
IF COL_LENGTH(N'dbo.employees', N'signature_valid_until') IS NULL
    ALTER TABLE dbo.employees ADD signature_valid_until DATE NULL;
GO
IF COL_LENGTH(N'dbo.employees', N'update_request_status') IS NULL
    ALTER TABLE dbo.employees ADD update_request_status BIT NULL
        CONSTRAINT df_employees_update_request DEFAULT (0) WITH VALUES;
GO

/* ---------- Employee request/approval workflow ---------- */

IF OBJECT_ID(N'dbo.employee_requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_requests
    (
        id                       BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_employee_requests PRIMARY KEY,
        requested_by             BIGINT NOT NULL,
        target_employee_id       BIGINT NULL,
        employee_code            VARCHAR(30) NOT NULL,
        employee_name            VARCHAR(100) NOT NULL,
        designation              BIGINT NULL,
        department               BIGINT NULL,
        branch                   BIGINT NULL,
        photo_path               VARCHAR(500) NOT NULL,
        signature_path           VARCHAR(500) NOT NULL,
        foreign_signature_path   VARCHAR(500) NULL,
        signature_valid_from     DATE NOT NULL,
        signature_valid_until    DATE NOT NULL,
        status                   VARCHAR(30) NOT NULL CONSTRAINT df_requests_status DEFAULT ('PENDING_DGM'),
        update_request_status    BIT NOT NULL CONSTRAINT df_requests_update_status DEFAULT (0),
        updated_after_rejection  BIT NOT NULL CONSTRAINT df_requests_rejected_update DEFAULT (0),
        remark                   VARCHAR(500) NOT NULL,
        requested_at             DATETIME2 NOT NULL CONSTRAINT df_requests_created DEFAULT (SYSDATETIME()),
        completed_at             DATETIME2 NULL,
        CONSTRAINT fk_requests_user FOREIGN KEY (requested_by) REFERENCES dbo.users(id),
        CONSTRAINT fk_requests_employee FOREIGN KEY (target_employee_id) REFERENCES dbo.employees(id),
        CONSTRAINT fk_requests_designation FOREIGN KEY (designation) REFERENCES dbo.Designation(DesignationId),
        CONSTRAINT fk_requests_department FOREIGN KEY (department) REFERENCES dbo.Department(DepartmentId),
        CONSTRAINT fk_requests_branch FOREIGN KEY (branch) REFERENCES dbo.branches(branch_id),
        CONSTRAINT ck_requests_status CHECK
            (status IN ('PENDING_DGM','PENDING_GM','APPROVED','REJECTED'))
    );
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'target_employee_id') IS NULL
    ALTER TABLE dbo.employee_requests ADD target_employee_id BIGINT NULL;
GO
IF COL_LENGTH(N'dbo.employee_requests', N'foreign_signature_path') IS NULL
    ALTER TABLE dbo.employee_requests ADD foreign_signature_path VARCHAR(500) NULL;
GO
IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_from') IS NULL
    ALTER TABLE dbo.employee_requests ADD signature_valid_from DATE NULL;
GO
IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_until') IS NULL
    ALTER TABLE dbo.employee_requests ADD signature_valid_until DATE NULL;
GO
IF COL_LENGTH(N'dbo.employee_requests', N'update_request_status') IS NULL
    ALTER TABLE dbo.employee_requests ADD update_request_status BIT NOT NULL
        CONSTRAINT df_requests_update_status DEFAULT (0) WITH VALUES;
GO
IF COL_LENGTH(N'dbo.employee_requests', N'updated_after_rejection') IS NULL
    ALTER TABLE dbo.employee_requests ADD updated_after_rejection BIT NOT NULL
        CONSTRAINT df_requests_rejected_update DEFAULT (0) WITH VALUES;
GO

IF OBJECT_ID(N'dbo.approval_history', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.approval_history
    (
        id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_approval_history PRIMARY KEY,
        request_id     BIGINT NOT NULL,
        acted_by       BIGINT NOT NULL,
        approval_level VARCHAR(10) NOT NULL,
        action         VARCHAR(20) NOT NULL,
        remark         VARCHAR(500) NOT NULL,
        action_at      DATETIME2 NOT NULL CONSTRAINT df_approval_created DEFAULT (SYSDATETIME()),
        CONSTRAINT fk_approval_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests(id),
        CONSTRAINT fk_approval_user FOREIGN KEY (acted_by) REFERENCES dbo.users(id),
        CONSTRAINT ck_approval_level CHECK (approval_level IN ('DGM','GM')),
        CONSTRAINT ck_approval_action CHECK (action IN ('APPROVED','REJECTED'))
    );
END;
GO

IF OBJECT_ID(N'dbo.employee_media_versions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_media_versions
    (
        id                     BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_media_versions PRIMARY KEY,
        employee_id            BIGINT NOT NULL,
        request_id             BIGINT NULL,
        version_number         INT NOT NULL,
        photo_path             VARCHAR(500) NOT NULL,
        signature_path         VARCHAR(500) NOT NULL,
        foreign_signature_path VARCHAR(500) NULL,
        approved_at            DATETIME2 NOT NULL CONSTRAINT df_media_approved DEFAULT (SYSDATETIME()),
        CONSTRAINT fk_media_employee FOREIGN KEY (employee_id) REFERENCES dbo.employees(id),
        CONSTRAINT fk_media_request FOREIGN KEY (request_id) REFERENCES dbo.employee_requests(id),
        CONSTRAINT uq_media_employee_version UNIQUE (employee_id, version_number)
    );
END;
GO

IF COL_LENGTH(N'dbo.employee_media_versions', N'foreign_signature_path') IS NULL
    ALTER TABLE dbo.employee_media_versions ADD foreign_signature_path VARCHAR(500) NULL;
GO

/* ---------- Indexes ---------- */

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employee_requests') AND name=N'ix_requests_target_employee')
    CREATE INDEX ix_requests_target_employee ON dbo.employee_requests(target_employee_id, status);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employee_requests') AND name=N'ix_requests_status_date')
    CREATE INDEX ix_requests_status_date ON dbo.employee_requests(status, requested_at);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employee_requests') AND name=N'ix_requests_code')
    CREATE INDEX ix_requests_code ON dbo.employee_requests(employee_code);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employees') AND name=N'ix_employees_name')
    CREATE INDEX ix_employees_name ON dbo.employees(full_name);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.approval_history') AND name=N'ix_approval_request')
    CREATE INDEX ix_approval_request ON dbo.approval_history(request_id, action_at);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employee_media_versions') AND name=N'ux_media_request_not_null')
    CREATE UNIQUE INDEX ux_media_request_not_null
        ON dbo.employee_media_versions(request_id) WHERE request_id IS NOT NULL;
GO

/* ---------- Idempotent seed data ---------- */

MERGE dbo.roles WITH (HOLDLOCK) AS target
USING (VALUES
    ('ADMIN',  'Manages users'),
    ('PD',     'Creates employee requests'),
    ('DGM',    'Level 1 approver'),
    ('GM',     'Level 2 approver'),
    ('BRANCH', 'Views approved employees')
) AS source(name, description)
ON target.name = source.name
WHEN NOT MATCHED THEN
    INSERT (name, description, active) VALUES (source.name, source.description, 1);
GO

MERGE dbo.branches WITH (HOLDLOCK) AS target
USING (VALUES
    (N'Head Office'), (N'Dhaka Branch'), (N'Chittagong Branch'),
    (N'Sylhet Branch'), (N'Rajshahi Branch'), (N'Khulna Branch')
) AS source(branch_name)
ON target.branch_name = source.branch_name
WHEN NOT MATCHED THEN INSERT (branch_name) VALUES (source.branch_name);
GO

MERGE dbo.Department WITH (HOLDLOCK) AS target
USING (VALUES
    ('Human Resources', 'Human Resources Department'),
    ('Information Technology', 'Information Technology Department'),
    ('Finance', 'Finance Department'),
    ('Accounts', 'Accounts Department'),
    ('Administration', 'Administration Department'),
    ('Audit', 'Audit Department'),
    ('Operations', 'Operations Department'),
    ('Credit', 'Credit Department'),
    ('General Banking', 'General Banking Department')
) AS source(name, description)
ON target.DepartmentName = source.name
WHEN NOT MATCHED THEN
    INSERT (DepartmentName, Description, IsActive, CreatedAt)
    VALUES (source.name, source.description, 1, SYSDATETIME());
GO

MERGE dbo.Designation WITH (HOLDLOCK) AS target
USING (VALUES
    ('Managing Director', 'Managing Director'),
    ('General Manager', 'General Manager'),
    ('Deputy General Manager', 'Deputy General Manager'),
    ('Senior Officer', 'Senior Officer'),
    ('Officer', 'Officer')
) AS source(name, description)
ON target.DesignationName = source.name
WHEN NOT MATCHED THEN
    INSERT (DesignationName, Description, IsActive, CreatedAt)
    VALUES (source.name, source.description, 1, SYSDATETIME());
GO

/*
 Upgrade legacy text relationships without losing their original values.
 The old columns are retained as nullable *_legacy columns. Distinct legacy
 values are inserted into the applicable lookup table before conversion.
*/

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employees'
             AND COLUMN_NAME='designation' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.Designation (DesignationName, Description, IsActive, CreatedAt)
    SELECT DISTINCT LTRIM(RTRIM(e.designation)), 'Migrated legacy value', 1, SYSDATETIME()
    FROM dbo.employees e
    WHERE NULLIF(LTRIM(RTRIM(e.designation)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.Designation d
                      WHERE d.DesignationName = LTRIM(RTRIM(e.designation)));
    IF COL_LENGTH('dbo.employees','designation_id_migration') IS NULL
        ALTER TABLE dbo.employees ADD designation_id_migration BIGINT NULL;
    EXEC(N'UPDATE e SET designation_id_migration=d.DesignationId
           FROM dbo.employees e JOIN dbo.Designation d
             ON d.DesignationName=LTRIM(RTRIM(e.designation))');
    EXEC sp_rename 'dbo.employees.designation', 'designation_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employees.designation_id_migration', 'designation', 'COLUMN';
    ALTER TABLE dbo.employees ALTER COLUMN designation_legacy VARCHAR(100) NULL;
    ALTER TABLE dbo.employees ALTER COLUMN designation BIGINT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employees'
             AND COLUMN_NAME='department' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.Department (DepartmentName, Description, IsActive, CreatedAt)
    SELECT DISTINCT LTRIM(RTRIM(e.department)), 'Migrated legacy value', 1, SYSDATETIME()
    FROM dbo.employees e
    WHERE NULLIF(LTRIM(RTRIM(e.department)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.Department d
                      WHERE d.DepartmentName=LTRIM(RTRIM(e.department)));
    IF COL_LENGTH('dbo.employees','department_id_migration') IS NULL
        ALTER TABLE dbo.employees ADD department_id_migration BIGINT NULL;
    EXEC(N'UPDATE e SET department_id_migration=d.DepartmentId
           FROM dbo.employees e JOIN dbo.Department d
             ON d.DepartmentName=LTRIM(RTRIM(e.department))');
    EXEC sp_rename 'dbo.employees.department', 'department_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employees.department_id_migration', 'department', 'COLUMN';
    ALTER TABLE dbo.employees ALTER COLUMN department_legacy VARCHAR(100) NULL;
    ALTER TABLE dbo.employees ALTER COLUMN department BIGINT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employees'
             AND COLUMN_NAME='branch_code' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.branches (branch_name)
    SELECT DISTINCT LTRIM(RTRIM(e.branch_code)) FROM dbo.employees e
    WHERE NULLIF(LTRIM(RTRIM(e.branch_code)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.branches b
                      WHERE b.branch_name=LTRIM(RTRIM(e.branch_code)));
    IF COL_LENGTH('dbo.employees','branch_id_migration') IS NULL
        ALTER TABLE dbo.employees ADD branch_id_migration BIGINT NULL;
    EXEC(N'UPDATE e SET branch_id_migration=b.branch_id
           FROM dbo.employees e JOIN dbo.branches b
             ON b.branch_name=LTRIM(RTRIM(e.branch_code))');
    EXEC sp_rename 'dbo.employees.branch_code', 'branch_code_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employees.branch_id_migration', 'branch_code', 'COLUMN';
    ALTER TABLE dbo.employees ALTER COLUMN branch_code_legacy VARCHAR(100) NULL;
    ALTER TABLE dbo.employees ALTER COLUMN branch_code BIGINT NOT NULL;
END;
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employee_requests'
             AND COLUMN_NAME='designation' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.Designation (DesignationName, Description, IsActive, CreatedAt)
    SELECT DISTINCT LTRIM(RTRIM(r.designation)), 'Migrated legacy value', 1, SYSDATETIME()
    FROM dbo.employee_requests r
    WHERE NULLIF(LTRIM(RTRIM(r.designation)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.Designation d
                      WHERE d.DesignationName=LTRIM(RTRIM(r.designation)));
    IF COL_LENGTH('dbo.employee_requests','designation_id_migration') IS NULL
        ALTER TABLE dbo.employee_requests ADD designation_id_migration BIGINT NULL;
    EXEC(N'UPDATE r SET designation_id_migration=d.DesignationId
           FROM dbo.employee_requests r JOIN dbo.Designation d
             ON d.DesignationName=LTRIM(RTRIM(r.designation))');
    EXEC sp_rename 'dbo.employee_requests.designation', 'designation_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employee_requests.designation_id_migration', 'designation', 'COLUMN';
    ALTER TABLE dbo.employee_requests ALTER COLUMN designation_legacy VARCHAR(100) NULL;
END;
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employee_requests'
             AND COLUMN_NAME='department' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.Department (DepartmentName, Description, IsActive, CreatedAt)
    SELECT DISTINCT LTRIM(RTRIM(r.department)), 'Migrated legacy value', 1, SYSDATETIME()
    FROM dbo.employee_requests r
    WHERE NULLIF(LTRIM(RTRIM(r.department)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.Department d
                      WHERE d.DepartmentName=LTRIM(RTRIM(r.department)));
    IF COL_LENGTH('dbo.employee_requests','department_id_migration') IS NULL
        ALTER TABLE dbo.employee_requests ADD department_id_migration BIGINT NULL;
    EXEC(N'UPDATE r SET department_id_migration=d.DepartmentId
           FROM dbo.employee_requests r JOIN dbo.Department d
             ON d.DepartmentName=LTRIM(RTRIM(r.department))');
    EXEC sp_rename 'dbo.employee_requests.department', 'department_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employee_requests.department_id_migration', 'department', 'COLUMN';
    ALTER TABLE dbo.employee_requests ALTER COLUMN department_legacy VARCHAR(100) NULL;
END;
GO

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
           WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employee_requests'
             AND COLUMN_NAME='branch' AND DATA_TYPE<>'bigint')
BEGIN
    INSERT INTO dbo.branches (branch_name)
    SELECT DISTINCT LTRIM(RTRIM(r.branch)) FROM dbo.employee_requests r
    WHERE NULLIF(LTRIM(RTRIM(r.branch)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.branches b
                      WHERE b.branch_name=LTRIM(RTRIM(r.branch)));
    IF COL_LENGTH('dbo.employee_requests','branch_id_migration') IS NULL
        ALTER TABLE dbo.employee_requests ADD branch_id_migration BIGINT NULL;
    EXEC(N'UPDATE r SET branch_id_migration=b.branch_id
           FROM dbo.employee_requests r JOIN dbo.branches b
             ON b.branch_name=LTRIM(RTRIM(r.branch))');
    EXEC sp_rename 'dbo.employee_requests.branch', 'branch_legacy', 'COLUMN';
    EXEC sp_rename 'dbo.employee_requests.branch_id_migration', 'branch', 'COLUMN';
    ALTER TABLE dbo.employee_requests ALTER COLUMN branch_legacy VARCHAR(100) NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_employees_designation')
    ALTER TABLE dbo.employees ADD CONSTRAINT fk_employees_designation
        FOREIGN KEY (designation) REFERENCES dbo.Designation(DesignationId);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_employees_department')
    ALTER TABLE dbo.employees ADD CONSTRAINT fk_employees_department
        FOREIGN KEY (department) REFERENCES dbo.Department(DepartmentId);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_employees_branch')
    ALTER TABLE dbo.employees ADD CONSTRAINT fk_employees_branch
        FOREIGN KEY (branch_code) REFERENCES dbo.branches(branch_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_requests_designation')
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_requests_designation
        FOREIGN KEY (designation) REFERENCES dbo.Designation(DesignationId);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_requests_department')
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_requests_department
        FOREIGN KEY (department) REFERENCES dbo.Department(DepartmentId);
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='fk_requests_branch')
    ALTER TABLE dbo.employee_requests ADD CONSTRAINT fk_requests_branch
        FOREIGN KEY (branch) REFERENCES dbo.branches(branch_id);
GO

PRINT 'Employee signature schema V1 is ready.';
GO
