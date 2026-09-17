USE [master]
GO
/****** Object:  Database [EmployeeSignatureDB]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE DATABASE [EmployeeSignatureDB]
 CONTAINMENT = NONE
 ON  PRIMARY
( NAME = N'EmployeeSignatureDB', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQL\DATA\EmployeeSignatureDB.mdf' , SIZE = 73728KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON
( NAME = N'EmployeeSignatureDB_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL17.SQLEXPRESS\MSSQL\DATA\EmployeeSignatureDB_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [EmployeeSignatureDB].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [EmployeeSignatureDB] SET ANSI_NULL_DEFAULT OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET ANSI_NULLS OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET ANSI_PADDING OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET ANSI_WARNINGS OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET ARITHABORT OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET AUTO_CLOSE ON
GO
ALTER DATABASE [EmployeeSignatureDB] SET AUTO_SHRINK OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET AUTO_UPDATE_STATISTICS ON
GO
ALTER DATABASE [EmployeeSignatureDB] SET CURSOR_CLOSE_ON_COMMIT OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET CURSOR_DEFAULT  GLOBAL
GO
ALTER DATABASE [EmployeeSignatureDB] SET CONCAT_NULL_YIELDS_NULL OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET NUMERIC_ROUNDABORT OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET QUOTED_IDENTIFIER OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET RECURSIVE_TRIGGERS OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET  ENABLE_BROKER
GO
ALTER DATABASE [EmployeeSignatureDB] SET AUTO_UPDATE_STATISTICS_ASYNC OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET DATE_CORRELATION_OPTIMIZATION OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET TRUSTWORTHY OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET ALLOW_SNAPSHOT_ISOLATION OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET PARAMETERIZATION SIMPLE
GO
ALTER DATABASE [EmployeeSignatureDB] SET READ_COMMITTED_SNAPSHOT OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET HONOR_BROKER_PRIORITY OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET RECOVERY SIMPLE
GO
ALTER DATABASE [EmployeeSignatureDB] SET  MULTI_USER
GO
ALTER DATABASE [EmployeeSignatureDB] SET PAGE_VERIFY CHECKSUM
GO
ALTER DATABASE [EmployeeSignatureDB] SET DB_CHAINING OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF )
GO
ALTER DATABASE [EmployeeSignatureDB] SET TARGET_RECOVERY_TIME = 60 SECONDS
GO
ALTER DATABASE [EmployeeSignatureDB] SET DELAYED_DURABILITY = DISABLED
GO
ALTER DATABASE [EmployeeSignatureDB] SET ACCELERATED_DATABASE_RECOVERY = OFF
GO
ALTER DATABASE [EmployeeSignatureDB] SET QUERY_STORE = ON
GO
ALTER DATABASE [EmployeeSignatureDB] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [EmployeeSignatureDB]
GO
/****** Object:  DatabaseRole [sms_app]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE ROLE [sms_app]
    GO
/****** Object:  Table [dbo].[approval_history]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[approval_history](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [request_id] [bigint] NOT NULL,
    [acted_by] [bigint] NOT NULL,
    [approval_level] [varchar](10) NOT NULL,
    [action] [varchar](20) NOT NULL,
    [remark] [varchar](500) NOT NULL,
    [action_at] [datetime2](7) NOT NULL,
    CONSTRAINT [pk_approval_history] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[audit_logs]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[audit_logs](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [user_id] [bigint] NULL,
    [username] [varchar](50) NULL,
    [action_type] [varchar](80) NOT NULL,
    [target_entity] [varchar](80) NOT NULL,
    [target_id] [varchar](80) NULL,
    [event_time] [datetime2](7) NOT NULL,
    [ip_address] [varchar](64) NULL,
    [old_value] [nvarchar](max) NULL,
    [new_value] [nvarchar](max) NULL,
    [result] [varchar](20) NOT NULL,
    [correlation_id] [varchar](80) NULL,
    [details] [nvarchar](1000) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[branches]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[branches](
    [branch_id] [bigint] IDENTITY(1,1) NOT NULL,
    [branch_name] [nvarchar](100) NOT NULL,
    [active] [bit] NOT NULL,
    [zone_name] [nvarchar](500) NULL,
    [branch_code] [varchar](10) NULL,
    [description] [varchar](500) NULL,
    PRIMARY KEY CLUSTERED
(
[branch_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[branch_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[Department]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[Department](
    [DepartmentId] [bigint] IDENTITY(1,1) NOT NULL,
    [DepartmentName] [varchar](200) NOT NULL,
    [Description] [varchar](500) NULL,
    [IsActive] [bit] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    CONSTRAINT [PK_Department] PRIMARY KEY CLUSTERED
(
[DepartmentId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[Designation]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[Designation](
    [DesignationId] [bigint] IDENTITY(1,1) NOT NULL,
    [DesignationName] [varchar](200) NOT NULL,
    [Description] [varchar](500) NULL,
    [IsActive] [bit] NOT NULL,
    [CreatedAt] [datetime2](7) NOT NULL,
    [HierarchyOrder] [int] NOT NULL,
    CONSTRAINT [PK_Designation] PRIMARY KEY CLUSTERED
(
[DesignationId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_change_proposals]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_change_proposals](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [requested_by] [bigint] NOT NULL,
    [justification] [varchar](500) NOT NULL,
    [proposed_data] [nvarchar](max) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [pd_comment] [varchar](500) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [active] [bit] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_media_requests]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_media_requests](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [submitted_by] [bigint] NOT NULL,
    [photo_path] [varchar](500) NULL,
    [local_signature_path] [varchar](500) NULL,
    [foreign_signature_path] [varchar](500) NULL,
    [status] [varchar](30) NOT NULL,
    [submitted_at] [datetime2](7) NOT NULL,
    [dgm_approver] [bigint] NULL,
    [dgm_decided_at] [datetime2](7) NULL,
    [dgm_remarks] [varchar](500) NULL,
    [gm_approver] [bigint] NULL,
    [gm_decided_at] [datetime2](7) NULL,
    [gm_remarks] [varchar](500) NULL,
    [rejection_remarks] [varchar](500) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_media_versions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_media_versions](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [request_id] [bigint] NULL,
    [version_number] [int] NOT NULL,
    [photo_path] [varchar](500) NULL,
    [signature_path] [varchar](500) NULL,
    [approved_at] [datetime2](7) NOT NULL,
    [foreign_signature_path] [nvarchar](500) NULL,
    CONSTRAINT [pk_employee_media_versions] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_employee_media_version] UNIQUE NONCLUSTERED
(
    [employee_id] ASC,
[version_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_requests]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_requests](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [requested_by] [bigint] NOT NULL,
    [employee_code] [varchar](30) NOT NULL,
    [employee_name] [varchar](100) NOT NULL,
    [designation_legacy] [varchar](100) NULL,
    [department_legacy] [varchar](100) NULL,
    [branch_legacy] [varchar](100) NULL,
    [photo_path] [varchar](500) NULL,
    [signature_path] [varchar](500) NULL,
    [status] [varchar](30) NOT NULL,
    [remark] [varchar](500) NOT NULL,
    [requested_at] [datetime2](7) NOT NULL,
    [completed_at] [datetime2](7) NULL,
    [target_employee_id] [bigint] NULL,
    [signature_valid_from] [date] NULL,
    [signature_valid_until] [date] NULL,
    [updated_after_rejection] [bit] NOT NULL,
    [update_request_status] [bit] NOT NULL,
    [local_signature_path] [varchar](500) NULL,
    [foreign_signature_path] [varchar](500) NULL,
    [status_id] [bigint] NULL,
    [designation] [bigint] NULL,
    [department] [bigint] NULL,
    [branch] [bigint] NULL,
    [change_proposal_id] [bigint] NULL,
    [classification] [varchar](10) NOT NULL,
    [joining_date] [date] NULL,
    CONSTRAINT [pk_employee_requests] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_status]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_status](
    [status_id] [bigint] IDENTITY(1,1) NOT NULL,
    [status_name] [varchar](20) NOT NULL,
    [active] [bit] NOT NULL,
    [display_order] [int] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[status_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[status_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employee_versions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employee_versions](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [version_no] [int] NOT NULL,
    [snapshot_json] [nvarchar](max) NOT NULL,
    [changed_by] [bigint] NULL,
    [reason] [varchar](500) NULL,
    [created_at] [datetime2](7) NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_employee_version] UNIQUE NONCLUSTERED
(
    [employee_id] ASC,
[version_no] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[employees]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[employees](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_number] [varchar](30) NOT NULL,
    [full_name] [varchar](100) NOT NULL,
    [designation_legacy] [varchar](100) NULL,
    [department_legacy] [varchar](100) NULL,
    [branch_code_legacy] [varchar](100) NULL,
    [photo_path] [varchar](500) NULL,
    [signature_path] [varchar](500) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [updated_at] [datetime2](7) NOT NULL,
    [signature_valid_from] [date] NULL,
    [signature_valid_until] [date] NULL,
    [update_request_status] [bit] NOT NULL,
    [local_signature_path] [varchar](500) NULL,
    [foreign_signature_path] [varchar](500) NULL,
    [employee_status_id] [bigint] NULL,
    [designation] [bigint] NOT NULL,
    [department] [bigint] NOT NULL,
    [branch_code] [bigint] NOT NULL,
    [locked] [bit] NOT NULL,
    [status_id] [bigint] NULL,
    [classification] [varchar](10) NOT NULL,
    [joining_date] [date] NULL,
    [batch_id] [bigint] NULL,
    [active] [bit] NOT NULL,
    CONSTRAINT [pk_employees] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_employees_number] UNIQUE NONCLUSTERED
(
[employee_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[flyway_schema_history]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[flyway_schema_history](
    [installed_rank] [int] NOT NULL,
    [version] [nvarchar](50) NULL,
    [description] [nvarchar](200) NULL,
    [type] [nvarchar](20) NOT NULL,
    [script] [nvarchar](1000) NOT NULL,
    [checksum] [int] NULL,
    [installed_by] [nvarchar](100) NOT NULL,
    [installed_on] [datetime] NOT NULL,
    [execution_time] [int] NOT NULL,
    [success] [bit] NOT NULL,
    CONSTRAINT [flyway_schema_history_pk] PRIMARY KEY CLUSTERED
(
[installed_rank] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[import_batch_items]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[import_batch_items](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [batch_id] [bigint] NOT NULL,
    [row_number] [int] NOT NULL,
    [row_data] [nvarchar](max) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [error_detail] [nvarchar](1000) NULL,
    [employee_id] [bigint] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_batch_row] UNIQUE NONCLUSTERED
(
    [batch_id] ASC,
[row_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[import_batches]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[import_batches](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [batch_number] [varchar](40) NOT NULL,
    [uploaded_by] [bigint] NOT NULL,
    [original_filename] [nvarchar](255) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [total_rows] [int] NOT NULL,
    [succeeded_rows] [int] NOT NULL,
    [failed_rows] [int] NOT NULL,
    [uploaded_at] [datetime2](7) NOT NULL,
    [active] [bit] NOT NULL,
    [original_file_path] [varchar](500) NULL,
    [dgm_comment] [varchar](500) NULL,
    [gm_comment] [varchar](500) NULL,
    [rejection_reason] [varchar](500) NULL,
    [retry_of_batch_id] [bigint] NULL,
    [dgm_decided_by] [bigint] NULL,
    [gm_decided_by] [bigint] NULL,
    [dgm_decided_at] [datetime2](7) NULL,
    [gm_decided_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[batch_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[permissions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[permissions](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [permission_key] [varchar](80) NOT NULL,
    [description] [varchar](300) NULL,
    [active] [bit] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_permissions_key] UNIQUE NONCLUSTERED
(
[permission_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[role_permissions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[role_permissions](
    [role_id] [bigint] NOT NULL,
    [permission_id] [bigint] NOT NULL,
    [granted_at] [datetime2](7) NOT NULL,
    [active] [bit] NOT NULL,
    [deactivated_at] [datetime2](7) NULL,
    CONSTRAINT [pk_role_permissions] PRIMARY KEY CLUSTERED
(
    [role_id] ASC,
[permission_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[roles]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[roles](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [name] [varchar](30) NOT NULL,
    [description] [varchar](200) NULL,
    [active] [bit] NOT NULL,
    [hierarchy_order] [int] NULL,
    [system_role] [bit] NOT NULL,
    CONSTRAINT [pk_roles] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_roles_name] UNIQUE NONCLUSTERED
(
[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_book_access]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_book_access](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [book_id] [bigint] NOT NULL,
    [user_id] [bigint] NULL,
    [role_id] [bigint] NULL,
    [granted_by] [bigint] NOT NULL,
    [granted_at] [datetime2](7) NOT NULL,
    [active] [bit] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_book_entries]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_book_entries](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [book_id] [bigint] NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [serial_number] [int] NOT NULL,
    [signature_type] [varchar](10) NOT NULL,
    [signature_path] [varchar](500) NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_book_entry_serial] UNIQUE NONCLUSTERED
(
    [book_id] ASC,
    [serial_number] ASC,
[signature_type] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_books]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_books](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [book_number] [varchar](40) NOT NULL,
    [book_year] [int] NOT NULL,
    [version_no] [int] NOT NULL,
    [signature_type] [varchar](10) NOT NULL,
    [file_path] [varchar](500) NOT NULL,
    [generated_by] [bigint] NOT NULL,
    [generated_at] [datetime2](7) NOT NULL,
    [active] [bit] NOT NULL,
    [file_sha256] [varchar](64) NULL,
    [status] [varchar](20) NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[book_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_book_version] UNIQUE NONCLUSTERED
(
    [book_year] ASC,
    [signature_type] ASC,
[version_no] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_change_proposals]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_change_proposals](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NOT NULL,
    [signature_type] [varchar](10) NOT NULL,
    [initiated_by] [bigint] NOT NULL,
    [initiator_remarks] [varchar](500) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [submitted_version_id] [bigint] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_types]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_types](
    [signature_type_id] [bigint] IDENTITY(1,1) NOT NULL,
    [signature_type_name] [varchar](20) NOT NULL,
    [active] [bit] NOT NULL,
    PRIMARY KEY CLUSTERED
(
[signature_type_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[signature_type_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_upload_batches]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_upload_batches](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [batch_number] [varchar](50) NOT NULL,
    [submitted_by] [bigint] NOT NULL,
    [total_files] [int] NOT NULL,
    [matched_files] [int] NOT NULL,
    [invalid_files] [int] NOT NULL,
    [status] [varchar](30) NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    UNIQUE NONCLUSTERED
(
[batch_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[signature_versions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[signature_versions](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [employee_id] [bigint] NULL,
    [employee_number] [varchar](30) NOT NULL,
    [signature_type] [varchar](10) NOT NULL,
    [version_number] [int] NOT NULL,
    [file_path] [varchar](500) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [current_approved] [bit] NOT NULL,
    [submitted_by] [bigint] NOT NULL,
    [submitted_at] [datetime2](7) NOT NULL,
    [batch_id] [bigint] NULL,
    [dgm_approver] [bigint] NULL,
    [dgm_decided_at] [datetime2](7) NULL,
    [dgm_remarks] [varchar](500) NULL,
    [gm_approver] [bigint] NULL,
    [gm_decided_at] [datetime2](7) NULL,
    [gm_remarks] [varchar](500) NULL,
    [rejection_remarks] [varchar](500) NULL,
    [change_proposal_id] [bigint] NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_signature_version] UNIQUE NONCLUSTERED
(
    [employee_number] ASC,
    [signature_type] ASC,
[version_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[students]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[students](
    [student_id] [int] NOT NULL,
    [student_name] [varchar](255) NULL,
    [age] [int] NULL,
    [gender] [varchar](255) NULL,
    [department_id] [int] NULL,
    [course_id] [int] NULL,
    [cgpa] [decimal](18, 0) NULL,
    PRIMARY KEY CLUSTERED
(
[student_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[system_settings]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[system_settings](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [setting_key] [varchar](80) NOT NULL,
    [setting_value] [nvarchar](1000) NOT NULL,
    [description] [nvarchar](300) NULL,
    [active] [bit] NOT NULL,
    [updated_by] [bigint] NULL,
    [updated_at] [datetime2](7) NOT NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_system_settings_key] UNIQUE NONCLUSTERED
(
[setting_key] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[user_creation_requests]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[user_creation_requests](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [proposed_by] [bigint] NOT NULL,
    [proposed_username] [varchar](50) NOT NULL,
    [proposed_password_hash] [varchar](255) NOT NULL,
    [proposed_full_name] [varchar](100) NOT NULL,
    [proposed_employee_number] [varchar](30) NOT NULL,
    [proposed_email] [varchar](100) NOT NULL,
    [proposed_branch_id] [varchar](100) NOT NULL,
    [proposed_role_id] [bigint] NOT NULL,
    [proposed_scope] [varchar](10) NOT NULL,
    [status] [varchar](30) NOT NULL,
    [rejection_reason] [varchar](500) NULL,
    [dgm_comment] [varchar](500) NULL,
    [gm_comment] [varchar](500) NULL,
    [created_at] [datetime2](7) NOT NULL,
    [decided_at] [datetime2](7) NULL,
    [active] [bit] NOT NULL,
    [dgm_decided_by] [bigint] NULL,
    [gm_decided_by] [bigint] NULL,
    [dgm_decided_at] [datetime2](7) NULL,
    [gm_decided_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[user_permissions]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[user_permissions](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [user_id] [bigint] NOT NULL,
    [permission_id] [bigint] NOT NULL,
    [allowed] [bit] NOT NULL,
    [active] [bit] NOT NULL,
    [granted_by] [bigint] NOT NULL,
    [granted_at] [datetime2](7) NOT NULL,
    [deactivated_at] [datetime2](7) NULL,
    PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_user_permissions] UNIQUE NONCLUSTERED
(
    [user_id] ASC,
[permission_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[users]    Script Date: 9/17/2026 12:23:24 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[users](
    [id] [bigint] IDENTITY(1,1) NOT NULL,
    [username] [varchar](50) NOT NULL,
    [password_hash] [varchar](255) NOT NULL,
    [full_name] [varchar](100) NOT NULL,
    [email] [varchar](100) NOT NULL,
    [role_id] [bigint] NOT NULL,
    [active] [bit] NOT NULL,
    [created_at] [datetime2](7) NOT NULL,
    [must_change_password] [bit] NOT NULL,
    [branch_id] [nvarchar](100) NOT NULL,
    [last_login_at] [datetime2](7) NULL,
    [employee_number] [varchar](30) NULL,
    [signature_scope] [varchar](10) NOT NULL,
    [approval_status] [varchar](30) NOT NULL,
    [deactivated_at] [datetime2](7) NULL,
    [created_by] [bigint] NULL,
    CONSTRAINT [pk_users] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_users_email] UNIQUE NONCLUSTERED
(
[email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [uq_users_username] UNIQUE NONCLUSTERED
(
[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Index [ix_approval_request]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_approval_request] ON [dbo].[approval_history]
(
	[request_id] ASC,
	[action_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_audit_action_time]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_audit_action_time] ON [dbo].[audit_logs]
(
	[action_type] ASC,
	[event_time] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_audit_entity]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_audit_entity] ON [dbo].[audit_logs]
(
	[target_entity] ASC,
	[target_id] ASC,
	[event_time] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ix_audit_event_time]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_audit_event_time] ON [dbo].[audit_logs]
(
	[event_time] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ix_audit_user_time]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_audit_user_time] ON [dbo].[audit_logs]
(
	[user_id] ASC,
	[event_time] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UX_Department_DepartmentName]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UX_Department_DepartmentName] ON [dbo].[Department]
(
	[DepartmentName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UX_Designation_DesignationName]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [UX_Designation_DesignationName] ON [dbo].[Designation]
(
	[DesignationName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_emr_status]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_emr_status] ON [dbo].[employee_media_requests]
(
	[status] ASC,
	[submitted_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ux_employee_media_request_not_null]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_employee_media_request_not_null] ON [dbo].[employee_media_versions]
(
	[request_id] ASC
)
WHERE ([request_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ux_media_request_not_null]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_media_request_not_null] ON [dbo].[employee_media_versions]
(
	[request_id] ASC
)
WHERE ([request_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_requests_code]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_requests_code] ON [dbo].[employee_requests]
(
	[employee_code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_requests_status_date]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_requests_status_date] ON [dbo].[employee_requests]
(
	[status] ASC,
	[requested_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_requests_target_employee]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_requests_target_employee] ON [dbo].[employee_requests]
(
	[target_employee_id] ASC,
	[status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_employees_name]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_employees_name] ON [dbo].[employees]
(
	[full_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [flyway_schema_history_s_idx]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [flyway_schema_history_s_idx] ON [dbo].[flyway_schema_history]
(
	[success] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ux_roles_active_hierarchy]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_roles_active_hierarchy] ON [dbo].[roles]
(
	[hierarchy_order] ASC
)
WHERE ([active]=(1) AND [hierarchy_order] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ux_book_access_role]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_book_access_role] ON [dbo].[signature_book_access]
(
	[book_id] ASC,
	[role_id] ASC
)
WHERE ([role_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ux_book_access_user]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_book_access_user] ON [dbo].[signature_book_access]
(
	[book_id] ASC,
	[user_id] ASC
)
WHERE ([user_id] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_scp_employee_type]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_scp_employee_type] ON [dbo].[signature_change_proposals]
(
	[employee_id] ASC,
	[signature_type] ASC,
	[status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_scp_pd_queue]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_scp_pd_queue] ON [dbo].[signature_change_proposals]
(
	[status] ASC,
	[created_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_sv_history]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_sv_history] ON [dbo].[signature_versions]
(
	[employee_number] ASC,
	[signature_type] ASC,
	[version_number] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_sv_pending]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_sv_pending] ON [dbo].[signature_versions]
(
	[status] ASC,
	[submitted_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [uq_sv_current]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [uq_sv_current] ON [dbo].[signature_versions]
(
	[employee_number] ASC,
	[signature_type] ASC
)
WHERE ([current_approved]=(1))
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ix_ucr_status]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_ucr_status] ON [dbo].[user_creation_requests]
(
	[status] ASC,
	[created_at] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [ix_user_permissions_lookup]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE NONCLUSTERED INDEX [ix_user_permissions_lookup] ON [dbo].[user_permissions]
(
	[user_id] ASC,
	[permission_id] ASC,
	[active] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [ux_users_employee_number]    Script Date: 9/17/2026 12:23:24 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [ux_users_employee_number] ON [dbo].[users]
(
	[employee_number] ASC
)
WHERE ([employee_number] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[approval_history] ADD  CONSTRAINT [df_approval_action_at]  DEFAULT (sysdatetime()) FOR [action_at]
    GO
ALTER TABLE [dbo].[audit_logs] ADD  CONSTRAINT [df_audit_time]  DEFAULT (sysutcdatetime()) FOR [event_time]
    GO
ALTER TABLE [dbo].[branches] ADD  CONSTRAINT [df_branches_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[Department] ADD  CONSTRAINT [DF_Department_IsActive]  DEFAULT ((1)) FOR [IsActive]
    GO
ALTER TABLE [dbo].[Department] ADD  CONSTRAINT [DF_Department_CreatedAt]  DEFAULT (sysdatetime()) FOR [CreatedAt]
    GO
ALTER TABLE [dbo].[Designation] ADD  CONSTRAINT [DF_Designation_IsActive]  DEFAULT ((1)) FOR [IsActive]
    GO
ALTER TABLE [dbo].[Designation] ADD  CONSTRAINT [DF_Designation_CreatedAt]  DEFAULT (sysdatetime()) FOR [CreatedAt]
    GO
ALTER TABLE [dbo].[Designation] ADD  CONSTRAINT [df_designation_hierarchy]  DEFAULT ((100)) FOR [HierarchyOrder]
    GO
ALTER TABLE [dbo].[employee_change_proposals] ADD  DEFAULT (sysutcdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[employee_change_proposals] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[employee_media_requests] ADD  DEFAULT ('PENDING_DGM') FOR [status]
    GO
ALTER TABLE [dbo].[employee_media_requests] ADD  DEFAULT (sysutcdatetime()) FOR [submitted_at]
    GO
ALTER TABLE [dbo].[employee_media_versions] ADD  CONSTRAINT [df_media_versions_approved_at]  DEFAULT (sysdatetime()) FOR [approved_at]
    GO
ALTER TABLE [dbo].[employee_requests] ADD  CONSTRAINT [df_requests_requested_at]  DEFAULT (sysdatetime()) FOR [requested_at]
    GO
ALTER TABLE [dbo].[employee_requests] ADD  CONSTRAINT [DF_employee_requests_updated_after_rejection]  DEFAULT ((0)) FOR [updated_after_rejection]
    GO
ALTER TABLE [dbo].[employee_requests] ADD  CONSTRAINT [DF_employee_requests_update_request_status]  DEFAULT ((0)) FOR [update_request_status]
    GO
ALTER TABLE [dbo].[employee_requests] ADD  CONSTRAINT [df_requests_classification]  DEFAULT ('BOTH') FOR [classification]
    GO
ALTER TABLE [dbo].[employee_status] ADD  CONSTRAINT [df_employee_status_active2]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[employee_status] ADD  CONSTRAINT [df_employee_status_order2]  DEFAULT ((100)) FOR [display_order]
    GO
ALTER TABLE [dbo].[employee_versions] ADD  DEFAULT (sysutcdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [df_employees_created_at]  DEFAULT (sysdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [df_employees_updated_at]  DEFAULT (sysdatetime()) FOR [updated_at]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [DF_employees_update_request_status]  DEFAULT ((0)) FOR [update_request_status]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [df_employees_locked]  DEFAULT ((0)) FOR [locked]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [df_employees_class]  DEFAULT ('BOTH') FOR [classification]
    GO
ALTER TABLE [dbo].[employees] ADD  CONSTRAINT [df_employees_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[flyway_schema_history] ADD  DEFAULT (getdate()) FOR [installed_on]
    GO
ALTER TABLE [dbo].[import_batches] ADD  DEFAULT ((0)) FOR [total_rows]
    GO
ALTER TABLE [dbo].[import_batches] ADD  DEFAULT ((0)) FOR [succeeded_rows]
    GO
ALTER TABLE [dbo].[import_batches] ADD  DEFAULT ((0)) FOR [failed_rows]
    GO
ALTER TABLE [dbo].[import_batches] ADD  DEFAULT (sysutcdatetime()) FOR [uploaded_at]
    GO
ALTER TABLE [dbo].[import_batches] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[permissions] ADD  CONSTRAINT [df_permissions_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[role_permissions] ADD  DEFAULT (sysutcdatetime()) FOR [granted_at]
    GO
ALTER TABLE [dbo].[role_permissions] ADD  CONSTRAINT [df_role_permissions_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[roles] ADD  CONSTRAINT [df_roles_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[roles] ADD  CONSTRAINT [df_roles_system]  DEFAULT ((0)) FOR [system_role]
    GO
ALTER TABLE [dbo].[signature_book_access] ADD  DEFAULT (sysutcdatetime()) FOR [granted_at]
    GO
ALTER TABLE [dbo].[signature_book_access] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[signature_books] ADD  DEFAULT (sysutcdatetime()) FOR [generated_at]
    GO
ALTER TABLE [dbo].[signature_books] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[signature_books] ADD  CONSTRAINT [df_signature_book_status]  DEFAULT ('CURRENT') FOR [status]
    GO
ALTER TABLE [dbo].[signature_change_proposals] ADD  DEFAULT ('PD_ACTION_REQUIRED') FOR [status]
    GO
ALTER TABLE [dbo].[signature_change_proposals] ADD  DEFAULT (sysutcdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[signature_types] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[signature_upload_batches] ADD  DEFAULT ((0)) FOR [total_files]
    GO
ALTER TABLE [dbo].[signature_upload_batches] ADD  DEFAULT ((0)) FOR [matched_files]
    GO
ALTER TABLE [dbo].[signature_upload_batches] ADD  DEFAULT ((0)) FOR [invalid_files]
    GO
ALTER TABLE [dbo].[signature_upload_batches] ADD  DEFAULT ('DRAFT') FOR [status]
    GO
ALTER TABLE [dbo].[signature_upload_batches] ADD  DEFAULT (sysutcdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[signature_versions] ADD  DEFAULT ((0)) FOR [current_approved]
    GO
ALTER TABLE [dbo].[signature_versions] ADD  DEFAULT (sysutcdatetime()) FOR [submitted_at]
    GO
ALTER TABLE [dbo].[system_settings] ADD  CONSTRAINT [df_system_settings_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[system_settings] ADD  CONSTRAINT [df_system_settings_updated]  DEFAULT (sysutcdatetime()) FOR [updated_at]
    GO
ALTER TABLE [dbo].[user_creation_requests] ADD  DEFAULT (sysutcdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[user_creation_requests] ADD  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[user_permissions] ADD  CONSTRAINT [df_user_permissions_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[user_permissions] ADD  CONSTRAINT [df_user_permissions_granted]  DEFAULT (sysutcdatetime()) FOR [granted_at]
    GO
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [df_users_active]  DEFAULT ((1)) FOR [active]
    GO
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [df_users_created_at]  DEFAULT (sysdatetime()) FOR [created_at]
    GO
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [df_users_must_change_password]  DEFAULT ((0)) FOR [must_change_password]
    GO
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [df_users_scope]  DEFAULT ('BOTH') FOR [signature_scope]
    GO
ALTER TABLE [dbo].[users] ADD  CONSTRAINT [df_users_approval]  DEFAULT ('APPROVED') FOR [approval_status]
    GO
ALTER TABLE [dbo].[approval_history]  WITH CHECK ADD  CONSTRAINT [fk_approval_request] FOREIGN KEY([request_id])
    REFERENCES [dbo].[employee_requests] ([id])
    GO
ALTER TABLE [dbo].[approval_history] CHECK CONSTRAINT [fk_approval_request]
    GO
ALTER TABLE [dbo].[approval_history]  WITH CHECK ADD  CONSTRAINT [fk_approval_user] FOREIGN KEY([acted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[approval_history] CHECK CONSTRAINT [fk_approval_user]
    GO
ALTER TABLE [dbo].[audit_logs]  WITH CHECK ADD  CONSTRAINT [fk_audit_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[audit_logs] CHECK CONSTRAINT [fk_audit_user]
    GO
ALTER TABLE [dbo].[employee_change_proposals]  WITH CHECK ADD  CONSTRAINT [fk_ecp_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[employee_change_proposals] CHECK CONSTRAINT [fk_ecp_employee]
    GO
ALTER TABLE [dbo].[employee_change_proposals]  WITH CHECK ADD  CONSTRAINT [fk_ecp_user] FOREIGN KEY([requested_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_change_proposals] CHECK CONSTRAINT [fk_ecp_user]
    GO
ALTER TABLE [dbo].[employee_media_requests]  WITH CHECK ADD  CONSTRAINT [fk_emr_dgm] FOREIGN KEY([dgm_approver])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_media_requests] CHECK CONSTRAINT [fk_emr_dgm]
    GO
ALTER TABLE [dbo].[employee_media_requests]  WITH CHECK ADD  CONSTRAINT [fk_emr_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[employee_media_requests] CHECK CONSTRAINT [fk_emr_employee]
    GO
ALTER TABLE [dbo].[employee_media_requests]  WITH CHECK ADD  CONSTRAINT [fk_emr_gm] FOREIGN KEY([gm_approver])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_media_requests] CHECK CONSTRAINT [fk_emr_gm]
    GO
ALTER TABLE [dbo].[employee_media_requests]  WITH CHECK ADD  CONSTRAINT [fk_emr_submitter] FOREIGN KEY([submitted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_media_requests] CHECK CONSTRAINT [fk_emr_submitter]
    GO
ALTER TABLE [dbo].[employee_media_versions]  WITH CHECK ADD  CONSTRAINT [fk_media_versions_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[employee_media_versions] CHECK CONSTRAINT [fk_media_versions_employee]
    GO
ALTER TABLE [dbo].[employee_media_versions]  WITH CHECK ADD  CONSTRAINT [fk_media_versions_request] FOREIGN KEY([request_id])
    REFERENCES [dbo].[employee_requests] ([id])
    GO
ALTER TABLE [dbo].[employee_media_versions] CHECK CONSTRAINT [fk_media_versions_request]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [FK_employee_requests_employee_status] FOREIGN KEY([status_id])
    REFERENCES [dbo].[employee_status] ([status_id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [FK_employee_requests_employee_status]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_request_change_proposal] FOREIGN KEY([change_proposal_id])
    REFERENCES [dbo].[employee_change_proposals] ([id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_request_change_proposal]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_request_employee_status] FOREIGN KEY([status_id])
    REFERENCES [dbo].[employee_status] ([status_id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_request_employee_status]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_requests_branch] FOREIGN KEY([branch])
    REFERENCES [dbo].[branches] ([branch_id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_requests_branch]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_requests_department] FOREIGN KEY([department])
    REFERENCES [dbo].[Department] ([DepartmentId])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_requests_department]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_requests_designation] FOREIGN KEY([designation])
    REFERENCES [dbo].[Designation] ([DesignationId])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_requests_designation]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_requests_target_employee] FOREIGN KEY([target_employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_requests_target_employee]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [fk_requests_user] FOREIGN KEY([requested_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [fk_requests_user]
    GO
ALTER TABLE [dbo].[employee_versions]  WITH CHECK ADD  CONSTRAINT [fk_ev_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[employee_versions] CHECK CONSTRAINT [fk_ev_employee]
    GO
ALTER TABLE [dbo].[employee_versions]  WITH CHECK ADD  CONSTRAINT [fk_ev_user] FOREIGN KEY([changed_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[employee_versions] CHECK CONSTRAINT [fk_ev_user]
    GO
ALTER TABLE [dbo].[employees]  WITH CHECK ADD  CONSTRAINT [fk_employees_batch] FOREIGN KEY([batch_id])
    REFERENCES [dbo].[import_batches] ([id])
    GO
ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [fk_employees_batch]
    GO
ALTER TABLE [dbo].[employees]  WITH CHECK ADD  CONSTRAINT [fk_employees_branch] FOREIGN KEY([branch_code])
    REFERENCES [dbo].[branches] ([branch_id])
    GO
ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [fk_employees_branch]
    GO
ALTER TABLE [dbo].[employees]  WITH CHECK ADD  CONSTRAINT [fk_employees_department] FOREIGN KEY([department])
    REFERENCES [dbo].[Department] ([DepartmentId])
    GO
ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [fk_employees_department]
    GO
ALTER TABLE [dbo].[employees]  WITH CHECK ADD  CONSTRAINT [fk_employees_designation] FOREIGN KEY([designation])
    REFERENCES [dbo].[Designation] ([DesignationId])
    GO
ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [fk_employees_designation]
    GO
ALTER TABLE [dbo].[employees]  WITH CHECK ADD  CONSTRAINT [fk_employees_status] FOREIGN KEY([employee_status_id])
    REFERENCES [dbo].[employee_status] ([status_id])
    GO
ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [fk_employees_status]
    GO
ALTER TABLE [dbo].[import_batch_items]  WITH CHECK ADD  CONSTRAINT [fk_item_batch] FOREIGN KEY([batch_id])
    REFERENCES [dbo].[import_batches] ([id])
    GO
ALTER TABLE [dbo].[import_batch_items] CHECK CONSTRAINT [fk_item_batch]
    GO
ALTER TABLE [dbo].[import_batch_items]  WITH CHECK ADD  CONSTRAINT [fk_item_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[import_batch_items] CHECK CONSTRAINT [fk_item_employee]
    GO
ALTER TABLE [dbo].[import_batches]  WITH CHECK ADD  CONSTRAINT [fk_batch_dgm_actor] FOREIGN KEY([dgm_decided_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[import_batches] CHECK CONSTRAINT [fk_batch_dgm_actor]
    GO
ALTER TABLE [dbo].[import_batches]  WITH CHECK ADD  CONSTRAINT [fk_batch_gm_actor] FOREIGN KEY([gm_decided_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[import_batches] CHECK CONSTRAINT [fk_batch_gm_actor]
    GO
ALTER TABLE [dbo].[import_batches]  WITH CHECK ADD  CONSTRAINT [fk_batch_retry_parent] FOREIGN KEY([retry_of_batch_id])
    REFERENCES [dbo].[import_batches] ([id])
    GO
ALTER TABLE [dbo].[import_batches] CHECK CONSTRAINT [fk_batch_retry_parent]
    GO
ALTER TABLE [dbo].[import_batches]  WITH CHECK ADD  CONSTRAINT [fk_batch_user] FOREIGN KEY([uploaded_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[import_batches] CHECK CONSTRAINT [fk_batch_user]
    GO
ALTER TABLE [dbo].[role_permissions]  WITH CHECK ADD  CONSTRAINT [fk_rp_permission] FOREIGN KEY([permission_id])
    REFERENCES [dbo].[permissions] ([id])
    GO
ALTER TABLE [dbo].[role_permissions] CHECK CONSTRAINT [fk_rp_permission]
    GO
ALTER TABLE [dbo].[role_permissions]  WITH CHECK ADD  CONSTRAINT [fk_rp_role] FOREIGN KEY([role_id])
    REFERENCES [dbo].[roles] ([id])
    GO
ALTER TABLE [dbo].[role_permissions] CHECK CONSTRAINT [fk_rp_role]
    GO
ALTER TABLE [dbo].[signature_book_access]  WITH CHECK ADD  CONSTRAINT [fk_ba_book] FOREIGN KEY([book_id])
    REFERENCES [dbo].[signature_books] ([id])
    GO
ALTER TABLE [dbo].[signature_book_access] CHECK CONSTRAINT [fk_ba_book]
    GO
ALTER TABLE [dbo].[signature_book_access]  WITH CHECK ADD  CONSTRAINT [fk_ba_granter] FOREIGN KEY([granted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_book_access] CHECK CONSTRAINT [fk_ba_granter]
    GO
ALTER TABLE [dbo].[signature_book_access]  WITH CHECK ADD  CONSTRAINT [fk_ba_role] FOREIGN KEY([role_id])
    REFERENCES [dbo].[roles] ([id])
    GO
ALTER TABLE [dbo].[signature_book_access] CHECK CONSTRAINT [fk_ba_role]
    GO
ALTER TABLE [dbo].[signature_book_access]  WITH CHECK ADD  CONSTRAINT [fk_ba_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_book_access] CHECK CONSTRAINT [fk_ba_user]
    GO
ALTER TABLE [dbo].[signature_book_entries]  WITH CHECK ADD  CONSTRAINT [fk_book_entry_book] FOREIGN KEY([book_id])
    REFERENCES [dbo].[signature_books] ([id])
    GO
ALTER TABLE [dbo].[signature_book_entries] CHECK CONSTRAINT [fk_book_entry_book]
    GO
ALTER TABLE [dbo].[signature_book_entries]  WITH CHECK ADD  CONSTRAINT [fk_book_entry_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[signature_book_entries] CHECK CONSTRAINT [fk_book_entry_employee]
    GO
ALTER TABLE [dbo].[signature_books]  WITH CHECK ADD  CONSTRAINT [fk_book_user] FOREIGN KEY([generated_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_books] CHECK CONSTRAINT [fk_book_user]
    GO
ALTER TABLE [dbo].[signature_change_proposals]  WITH CHECK ADD  CONSTRAINT [fk_scp_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[signature_change_proposals] CHECK CONSTRAINT [fk_scp_employee]
    GO
ALTER TABLE [dbo].[signature_change_proposals]  WITH CHECK ADD  CONSTRAINT [fk_scp_initiator] FOREIGN KEY([initiated_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_change_proposals] CHECK CONSTRAINT [fk_scp_initiator]
    GO
ALTER TABLE [dbo].[signature_change_proposals]  WITH CHECK ADD  CONSTRAINT [fk_scp_version] FOREIGN KEY([submitted_version_id])
    REFERENCES [dbo].[signature_versions] ([id])
    GO
ALTER TABLE [dbo].[signature_change_proposals] CHECK CONSTRAINT [fk_scp_version]
    GO
ALTER TABLE [dbo].[signature_upload_batches]  WITH CHECK ADD  CONSTRAINT [fk_signature_batch_user] FOREIGN KEY([submitted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_upload_batches] CHECK CONSTRAINT [fk_signature_batch_user]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_batch] FOREIGN KEY([batch_id])
    REFERENCES [dbo].[signature_upload_batches] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_batch]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_change_proposal] FOREIGN KEY([change_proposal_id])
    REFERENCES [dbo].[signature_change_proposals] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_change_proposal]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_dgm] FOREIGN KEY([dgm_approver])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_dgm]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_employee] FOREIGN KEY([employee_id])
    REFERENCES [dbo].[employees] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_employee]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_gm] FOREIGN KEY([gm_approver])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_gm]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [fk_sv_submitter] FOREIGN KEY([submitted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [fk_sv_submitter]
    GO
ALTER TABLE [dbo].[system_settings]  WITH CHECK ADD  CONSTRAINT [fk_system_settings_user] FOREIGN KEY([updated_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[system_settings] CHECK CONSTRAINT [fk_system_settings_user]
    GO
ALTER TABLE [dbo].[user_creation_requests]  WITH CHECK ADD  CONSTRAINT [fk_ucr_dgm_actor] FOREIGN KEY([dgm_decided_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[user_creation_requests] CHECK CONSTRAINT [fk_ucr_dgm_actor]
    GO
ALTER TABLE [dbo].[user_creation_requests]  WITH CHECK ADD  CONSTRAINT [fk_ucr_gm_actor] FOREIGN KEY([gm_decided_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[user_creation_requests] CHECK CONSTRAINT [fk_ucr_gm_actor]
    GO
ALTER TABLE [dbo].[user_creation_requests]  WITH CHECK ADD  CONSTRAINT [fk_ucr_proposer] FOREIGN KEY([proposed_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[user_creation_requests] CHECK CONSTRAINT [fk_ucr_proposer]
    GO
ALTER TABLE [dbo].[user_creation_requests]  WITH CHECK ADD  CONSTRAINT [fk_ucr_role] FOREIGN KEY([proposed_role_id])
    REFERENCES [dbo].[roles] ([id])
    GO
ALTER TABLE [dbo].[user_creation_requests] CHECK CONSTRAINT [fk_ucr_role]
    GO
ALTER TABLE [dbo].[user_permissions]  WITH CHECK ADD  CONSTRAINT [fk_up_granter] FOREIGN KEY([granted_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[user_permissions] CHECK CONSTRAINT [fk_up_granter]
    GO
ALTER TABLE [dbo].[user_permissions]  WITH CHECK ADD  CONSTRAINT [fk_up_permission] FOREIGN KEY([permission_id])
    REFERENCES [dbo].[permissions] ([id])
    GO
ALTER TABLE [dbo].[user_permissions] CHECK CONSTRAINT [fk_up_permission]
    GO
ALTER TABLE [dbo].[user_permissions]  WITH CHECK ADD  CONSTRAINT [fk_up_user] FOREIGN KEY([user_id])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[user_permissions] CHECK CONSTRAINT [fk_up_user]
    GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [fk_users_created_by] FOREIGN KEY([created_by])
    REFERENCES [dbo].[users] ([id])
    GO
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [fk_users_created_by]
    GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD  CONSTRAINT [fk_users_role] FOREIGN KEY([role_id])
    REFERENCES [dbo].[roles] ([id])
    GO
ALTER TABLE [dbo].[users] CHECK CONSTRAINT [fk_users_role]
    GO
ALTER TABLE [dbo].[approval_history]  WITH CHECK ADD  CONSTRAINT [ck_approval_action] CHECK  (([action]='REJECTED' OR [action]='APPROVED'))
    GO
ALTER TABLE [dbo].[approval_history] CHECK CONSTRAINT [ck_approval_action]
    GO
ALTER TABLE [dbo].[approval_history]  WITH CHECK ADD  CONSTRAINT [ck_approval_level] CHECK  (([approval_level]='GM' OR [approval_level]='DGM'))
    GO
ALTER TABLE [dbo].[approval_history] CHECK CONSTRAINT [ck_approval_level]
    GO
ALTER TABLE [dbo].[employee_media_requests]  WITH CHECK ADD  CONSTRAINT [ck_media_request_has_file] CHECK  (([photo_path] IS NOT NULL OR [local_signature_path] IS NOT NULL OR [foreign_signature_path] IS NOT NULL))
    GO
ALTER TABLE [dbo].[employee_media_requests] CHECK CONSTRAINT [ck_media_request_has_file]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [ck_requests_signature_dates] CHECK  (([signature_valid_until]>=[signature_valid_from]))
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [ck_requests_signature_dates]
    GO
ALTER TABLE [dbo].[employee_requests]  WITH CHECK ADD  CONSTRAINT [ck_requests_status] CHECK  (([status]='REJECTED' OR [status]='APPROVED' OR [status]='PENDING_GM' OR [status]='PENDING_DGM'))
    GO
ALTER TABLE [dbo].[employee_requests] CHECK CONSTRAINT [ck_requests_status]
    GO
ALTER TABLE [dbo].[signature_book_access]  WITH CHECK ADD  CONSTRAINT [ck_ba_subject] CHECK  (([user_id] IS NULL AND [role_id] IS NOT NULL OR [user_id] IS NOT NULL AND [role_id] IS NULL))
    GO
ALTER TABLE [dbo].[signature_book_access] CHECK CONSTRAINT [ck_ba_subject]
    GO
ALTER TABLE [dbo].[signature_change_proposals]  WITH CHECK ADD  CONSTRAINT [ck_signature_change_type] CHECK  (([signature_type]='FOREIGN' OR [signature_type]='LOCAL'))
    GO
ALTER TABLE [dbo].[signature_change_proposals] CHECK CONSTRAINT [ck_signature_change_type]
    GO
ALTER TABLE [dbo].[signature_versions]  WITH CHECK ADD  CONSTRAINT [ck_signature_type] CHECK  (([signature_type]='FOREIGN' OR [signature_type]='LOCAL'))
    GO
ALTER TABLE [dbo].[signature_versions] CHECK CONSTRAINT [ck_signature_type]
    GO
    USE [master]
    GO
ALTER DATABASE [EmployeeSignatureDB] SET  READ_WRITE
GO
