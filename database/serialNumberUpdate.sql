/*=============================================================================
  EMPLOYEE SIGNATURE SERIAL NUMBERS
  ---------------------------------------------------------------------------
  Stores yearly signature serial numbers separately from dbo.employees.

  employee_id       -> dbo.employees.id
  serial_number_2025 -> unique integer for 2025
  serial_number_2026 -> unique integer for 2026

  The serial numbers are NOT primary keys.
  Each year's serial number is independently unique.

  NULL is allowed when an employee has not yet received a serial number.
=============================================================================*/

IF OBJECT_ID(N'dbo.employee_signature_serial_numbers', N'U') IS NULL
BEGIN
CREATE TABLE dbo.employee_signature_serial_numbers
(
    employee_id         BIGINT NOT NULL,
    serial_number_2025  INT NULL,
    serial_number_2026  INT NULL,

    CONSTRAINT PK_employee_signature_serial_numbers
        PRIMARY KEY (employee_id),

    CONSTRAINT FK_employee_signature_serial_numbers_employee
        FOREIGN KEY (employee_id)
            REFERENCES dbo.employees (id)
);
END;
GO

/*-----------------------------------------------------------------------------
  2025 serial number must be unique.
  Multiple NULL values are allowed.
-----------------------------------------------------------------------------*/
IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_signature_serial_numbers')
      AND name = N'UX_employee_signature_serial_numbers_2025'
)
BEGIN
CREATE UNIQUE INDEX UX_employee_signature_serial_numbers_2025
    ON dbo.employee_signature_serial_numbers (serial_number_2025)
    WHERE serial_number_2025 IS NOT NULL;
END;
GO

/*-----------------------------------------------------------------------------
  2026 serial number must be unique.
  Multiple NULL values are allowed.
-----------------------------------------------------------------------------*/
IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.employee_signature_serial_numbers')
      AND name = N'UX_employee_signature_serial_numbers_2026'
)
BEGIN
CREATE UNIQUE INDEX UX_employee_signature_serial_numbers_2026
    ON dbo.employee_signature_serial_numbers (serial_number_2026)
    WHERE serial_number_2026 IS NOT NULL;
END;
GO
