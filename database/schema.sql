/* ============================================================
   Employee Signature Management System
   Microsoft SQL Server
   Department + Designation + Branch + Signature Type + Status
   Idempotent Upgrade Script
   ============================================================ */

USE [master];
GO

/* ============================================================
   DATABASE
   ============================================================ */

IF DB_ID(N'EmployeeSignatureDB') IS NULL
BEGIN
    CREATE DATABASE [EmployeeSignatureDB];
END;
GO

USE [EmployeeSignatureDB];
GO


/* ============================================================
   ROLES
   ============================================================ */

IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
CREATE TABLE dbo.roles
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(200) NULL,
    active BIT NOT NULL
        CONSTRAINT DF_roles_active DEFAULT (1),

    CONSTRAINT PK_roles
        PRIMARY KEY (id),

    CONSTRAINT UQ_roles_name
        UNIQUE (name)
);
END;
GO


/* ============================================================
   USERS
   ============================================================ */

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
CREATE TABLE dbo.users
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role_id BIGINT NOT NULL,
    active BIT NOT NULL
        CONSTRAINT DF_users_active DEFAULT (1),
    must_change_password BIT NOT NULL
        CONSTRAINT DF_users_must_change_password DEFAULT (1),
    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_users_created_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_users
        PRIMARY KEY (id),

    CONSTRAINT UQ_users_username
        UNIQUE (username),

    CONSTRAINT UQ_users_email
        UNIQUE (email),

    CONSTRAINT FK_users_role
        FOREIGN KEY (role_id)
            REFERENCES dbo.roles(id)
);
END;
GO


/* Add must_change_password if missing */

IF COL_LENGTH(N'dbo.users', N'must_change_password') IS NULL
BEGIN
ALTER TABLE dbo.users
    ADD must_change_password BIT NOT NULL
    CONSTRAINT DF_users_must_change_password
    DEFAULT (1) WITH VALUES;
END;
GO


/* ============================================================
   BRANCHES
   ============================================================ */

IF OBJECT_ID(N'dbo.branches', N'U') IS NULL
BEGIN
CREATE TABLE dbo.branches
(
    branch_id BIGINT IDENTITY(1,1) NOT NULL,
    branch_name NVARCHAR(100) NOT NULL,

    CONSTRAINT PK_branches
        PRIMARY KEY (branch_id),

    CONSTRAINT UQ_branches_name
        UNIQUE (branch_name)
);
END;
GO


/* Add branch_id to users only if missing */

IF COL_LENGTH(N'dbo.users', N'branch_id') IS NULL
BEGIN
ALTER TABLE dbo.users
    ADD branch_id BIGINT NULL;
END;
GO


/* Insert branches only if they don't exist */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Head Office'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Head Office');
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Dhaka Branch'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Dhaka Branch');
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Chittagong Branch'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Chittagong Branch');
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Sylhet Branch'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Sylhet Branch');
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Rajshahi Branch'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Rajshahi Branch');
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.branches
    WHERE branch_name = N'Khulna Branch'
)
BEGIN
INSERT INTO dbo.branches (branch_name)
VALUES (N'Khulna Branch');
END;
GO


/* ============================================================
   DEPARTMENT
   ============================================================ */

IF OBJECT_ID(N'dbo.Department', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Department
(
    DepartmentId BIGINT IDENTITY(1,1) NOT NULL,
    DepartmentName VARCHAR(200) NOT NULL,
    Description VARCHAR(500) NULL,
    IsActive BIT NOT NULL
        CONSTRAINT DF_Department_IsActive DEFAULT (1),
    CreatedAt DATETIME2 NOT NULL
        CONSTRAINT DF_Department_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Department
        PRIMARY KEY (DepartmentId),

    CONSTRAINT UQ_Department_DepartmentName
        UNIQUE (DepartmentName)
);
END;
GO


/* ============================================================
   DESIGNATION
   ============================================================ */

IF OBJECT_ID(N'dbo.Designation', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Designation
(
    DesignationId BIGINT IDENTITY(1,1) NOT NULL,
    DesignationName VARCHAR(200) NOT NULL,
    Description VARCHAR(500) NULL,
    IsActive BIT NOT NULL
        CONSTRAINT DF_Designation_IsActive DEFAULT (1),
    CreatedAt DATETIME2 NOT NULL
        CONSTRAINT DF_Designation_CreatedAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Designation
        PRIMARY KEY (DesignationId),

    CONSTRAINT UQ_Designation_DesignationName
        UNIQUE (DesignationName)
);
END;
GO


/* ============================================================
   DEPARTMENT DATA
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Human Resources'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Human Resources', 'Human Resources Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Information Technology'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Information Technology', 'Information Technology Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Finance'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Finance', 'Finance Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Accounts'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Accounts', 'Accounts Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Administration'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Administration', 'Administration Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Audit'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Audit', 'Audit Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Operations'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Operations', 'Operations Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'Credit'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('Credit', 'Credit Department', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Department
    WHERE DepartmentName = 'General Banking'
)
INSERT INTO dbo.Department
    (DepartmentName, Description, IsActive)
VALUES
    ('General Banking', 'General Banking Department', 1);
GO


/* ============================================================
   DESIGNATION DATA
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Designation
    WHERE DesignationName = 'Managing Director'
)
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive)
VALUES
    ('Managing Director', 'Managing Director', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Designation
    WHERE DesignationName = 'General Manager'
)
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive)
VALUES
    ('General Manager', 'General Manager', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Designation
    WHERE DesignationName = 'Deputy General Manager'
)
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive)
VALUES
    ('Deputy General Manager', 'Deputy General Manager', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Designation
    WHERE DesignationName = 'Senior Officer'
)
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive)
VALUES
    ('Senior Officer', 'Senior Officer', 1);
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.Designation
    WHERE DesignationName = 'Officer'
)
INSERT INTO dbo.Designation
    (DesignationName, Description, IsActive)
VALUES
    ('Officer', 'Officer', 1);
GO


/* ============================================================
   EMPLOYEE REQUESTS
   ============================================================ */

IF OBJECT_ID(N'dbo.employee_requests', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_requests
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    requested_by BIGINT NOT NULL,
    employee_code VARCHAR(30) NOT NULL,
    employee_name VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    branch VARCHAR(100) NOT NULL,
    photo_path VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    remark VARCHAR(500) NOT NULL,

    requested_at DATETIME2 NOT NULL
        CONSTRAINT DF_requests_requested_at
        DEFAULT (SYSDATETIME()),

    completed_at DATETIME2 NULL,

    CONSTRAINT PK_employee_requests
        PRIMARY KEY (id),

    CONSTRAINT FK_requests_user
        FOREIGN KEY (requested_by)
            REFERENCES dbo.users(id),

    CONSTRAINT CK_requests_status
        CHECK
            (
            status IN
            (
             'PENDING_DGM',
             'PENDING_GM',
             'APPROVED',
             'REJECTED'
                )
            )
);
END;
GO


/* ============================================================
   EMPLOYEES
   ============================================================ */

IF OBJECT_ID(N'dbo.employees', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employees
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    employee_number VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    branch_code VARCHAR(100) NOT NULL,

    photo_path VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,

    created_at DATETIME2 NOT NULL
        CONSTRAINT DF_employees_created_at
        DEFAULT (SYSDATETIME()),

    updated_at DATETIME2 NOT NULL
        CONSTRAINT DF_employees_updated_at
        DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_employees
        PRIMARY KEY (id),

    CONSTRAINT UQ_employees_number
        UNIQUE (employee_number)
);
END;
GO


/* ============================================================
   TARGET EMPLOYEE ID
   ============================================================ */

IF COL_LENGTH(N'dbo.employee_requests', N'target_employee_id') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD target_employee_id BIGINT NULL;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_requests_target_employee'
      AND parent_object_id = OBJECT_ID(N'dbo.employee_requests')
)
BEGIN
ALTER TABLE dbo.employee_requests
    ADD CONSTRAINT FK_requests_target_employee
        FOREIGN KEY (target_employee_id)
            REFERENCES dbo.employees(id);
END;
GO


/* ============================================================
   APPROVAL HISTORY
   ============================================================ */

IF OBJECT_ID(N'dbo.approval_history', N'U') IS NULL
BEGIN
CREATE TABLE dbo.approval_history
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    request_id BIGINT NOT NULL,
    acted_by BIGINT NOT NULL,
    approval_level VARCHAR(10) NOT NULL,
    action VARCHAR(20) NOT NULL,
    remark VARCHAR(500) NOT NULL,

    action_at DATETIME2 NOT NULL
        CONSTRAINT DF_approval_action_at
        DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_approval_history
        PRIMARY KEY (id),

    CONSTRAINT FK_approval_request
        FOREIGN KEY (request_id)
            REFERENCES dbo.employee_requests(id),

    CONSTRAINT FK_approval_user
        FOREIGN KEY (acted_by)
            REFERENCES dbo.users(id),

    CONSTRAINT CK_approval_level
        CHECK (approval_level IN ('DGM', 'GM')),

    CONSTRAINT CK_approval_action
        CHECK (action IN ('APPROVED', 'REJECTED'))
    );
END;
GO


/* ============================================================
   EMPLOYEE MEDIA VERSIONS
   ============================================================ */

IF OBJECT_ID(N'dbo.employee_media_versions', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_media_versions
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    employee_id BIGINT NOT NULL,
    request_id BIGINT NULL,
    version_number INT NOT NULL,
    photo_path VARCHAR(500) NOT NULL,
    signature_path VARCHAR(500) NOT NULL,

    approved_at DATETIME2 NOT NULL
        CONSTRAINT DF_media_versions_approved_at
        DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_employee_media_versions
        PRIMARY KEY (id),

    CONSTRAINT FK_media_versions_employee
        FOREIGN KEY (employee_id)
            REFERENCES dbo.employees(id),

    CONSTRAINT FK_media_versions_request
        FOREIGN KEY (request_id)
            REFERENCES dbo.employee_requests(id),

    CONSTRAINT UQ_employee_media_version
        UNIQUE (employee_id, version_number)
);
END;
GO


/* ============================================================
   SIGNATURE VALIDITY
   ============================================================ */

IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_from') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD signature_valid_from DATE NULL;
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'signature_valid_until') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD signature_valid_until DATE NULL;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'signature_valid_from') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD signature_valid_from DATE NULL;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'signature_valid_until') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD signature_valid_until DATE NULL;
END;
GO


/* ============================================================
   REJECTION / UPDATE STATUS
   ============================================================ */

IF COL_LENGTH(N'dbo.employee_requests', N'updated_after_rejection') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD updated_after_rejection BIT NOT NULL
    CONSTRAINT DF_employee_requests_updated_after_rejection
    DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'update_request_status') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employee_requests_update_request_status
    DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'update_request_status') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD update_request_status BIT NOT NULL
    CONSTRAINT DF_employees_update_request_status
    DEFAULT (0) WITH VALUES;
END;
GO


/* ============================================================
   SIGNATURE TYPES
   ============================================================ */

IF OBJECT_ID(N'dbo.signature_types', N'U') IS NULL
BEGIN
CREATE TABLE dbo.signature_types
(
    signature_type_id BIGINT IDENTITY(1,1) NOT NULL,
    signature_type_name VARCHAR(20) NOT NULL,
    active BIT NOT NULL DEFAULT (1),

    CONSTRAINT PK_signature_types
        PRIMARY KEY (signature_type_id),

    CONSTRAINT UQ_signature_types_name
        UNIQUE (signature_type_name)
);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.signature_types
    WHERE signature_type_name = 'Local'
)
BEGIN
INSERT INTO dbo.signature_types (signature_type_name)
VALUES ('Local');
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.signature_types
    WHERE signature_type_name = 'Foreign'
)
BEGIN
INSERT INTO dbo.signature_types (signature_type_name)
VALUES ('Foreign');
END;
GO


/* ============================================================
   LOCAL / FOREIGN SIGNATURE PATH
   ============================================================ */

IF COL_LENGTH(N'dbo.employee_requests', N'local_signature_path') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD local_signature_path VARCHAR(500) NULL;
END;
GO

IF COL_LENGTH(N'dbo.employee_requests', N'foreign_signature_path') IS NULL
BEGIN
ALTER TABLE dbo.employee_requests
    ADD foreign_signature_path VARCHAR(500) NULL;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'local_signature_path') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD local_signature_path VARCHAR(500) NULL;
END;
GO

IF COL_LENGTH(N'dbo.employees', N'foreign_signature_path') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD foreign_signature_path VARCHAR(500) NULL;
END;
GO


/* ============================================================
   EMPLOYEE STATUS
   ============================================================ */

IF OBJECT_ID(N'dbo.employee_status', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_status
(
    status_id BIGINT IDENTITY(1,1) NOT NULL,
    status_name VARCHAR(20) NOT NULL,

    CONSTRAINT PK_employee_status
        PRIMARY KEY (status_id),

    CONSTRAINT UQ_employee_status_name
        UNIQUE (status_name)
);
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.employee_status
    WHERE status_name = 'Active'
)
BEGIN
INSERT INTO dbo.employee_status (status_name)
VALUES ('Active');
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.employee_status
    WHERE status_name = 'Inactive'
)
BEGIN
INSERT INTO dbo.employee_status (status_name)
VALUES ('Inactive');
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM dbo.employee_status
    WHERE status_name = 'Resign'
)
BEGIN
INSERT INTO dbo.employee_status (status_name)
VALUES ('Resign');
END;
GO


/* ============================================================
   EMPLOYEE STATUS ID
   ============================================================ */

IF COL_LENGTH(N'dbo.employees', N'employee_status_id') IS NULL
BEGIN
ALTER TABLE dbo.employees
    ADD employee_status_id BIGINT NULL;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_employees_status'
      AND parent_object_id = OBJECT_ID(N'dbo.employees')
)
BEGIN
ALTER TABLE dbo.employees
    ADD CONSTRAINT FK_employees_status
        FOREIGN KEY (employee_status_id)
            REFERENCES dbo.employee_status(status_id);
END;
GO


/* ============================================================
   INDEXES
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_requests_target_employee'
      AND object_id = OBJECT_ID(N'dbo.employee_requests')
)
BEGIN
CREATE INDEX IX_requests_target_employee
    ON dbo.employee_requests(target_employee_id, status);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_requests_status_date'
      AND object_id = OBJECT_ID(N'dbo.employee_requests')
)
BEGIN
CREATE INDEX IX_requests_status_date
    ON dbo.employee_requests(status, requested_at);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_requests_code'
      AND object_id = OBJECT_ID(N'dbo.employee_requests')
)
BEGIN
CREATE INDEX IX_requests_code
    ON dbo.employee_requests(employee_code);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_employees_name'
      AND object_id = OBJECT_ID(N'dbo.employees')
)
BEGIN
CREATE INDEX IX_employees_name
    ON dbo.employees(full_name);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_approval_request'
      AND object_id = OBJECT_ID(N'dbo.approval_history')
)
BEGIN
CREATE INDEX IX_approval_request
    ON dbo.approval_history(request_id, action_at);
END;
GO


/* ============================================================
   ROLES DATA
   ============================================================ */

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'ADMIN')
BEGIN
INSERT INTO dbo.roles (name, description, active)
VALUES ('ADMIN', 'Manages users', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'PD')
BEGIN
INSERT INTO dbo.roles (name, description, active)
VALUES ('PD', 'Creates employee requests', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'DGM')
BEGIN
INSERT INTO dbo.roles (name, description, active)
VALUES ('DGM', 'Level 1 approver', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'GM')
BEGIN
INSERT INTO dbo.roles (name, description, active)
VALUES ('GM', 'Level 2 approver', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = 'BRANCH')
BEGIN
INSERT INTO dbo.roles (name, description, active)
VALUES ('BRANCH', 'Views approved employees', 1);
END;
GO


/* ============================================================
   FINAL CHECK
   ============================================================ */

SELECT
    'roles' AS TableName,
    COUNT(*) AS TotalRows
FROM dbo.roles

UNION ALL

SELECT
    'users',
    COUNT(*)
FROM dbo.users

UNION ALL

SELECT
    'branches',
    COUNT(*)
FROM dbo.branches

UNION ALL

SELECT
    'Department',
    COUNT(*)
FROM dbo.Department

UNION ALL

SELECT
    'Designation',
    COUNT(*)
FROM dbo.Designation

UNION ALL

SELECT
    'employee_requests',
    COUNT(*)
FROM dbo.employee_requests

UNION ALL

SELECT
    'employees',
    COUNT(*)
FROM dbo.employees

UNION ALL

SELECT
    'signature_types',
    COUNT(*)
FROM dbo.signature_types

UNION ALL

SELECT
    'employee_status',
    COUNT(*)
FROM dbo.employee_status;
GO