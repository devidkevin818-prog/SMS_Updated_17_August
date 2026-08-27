package com.bank.signaturemanagement.dto;

public class EmployeeSignatureSerialNumberDto {

    private Long employeeId;
    private String employeeNumber;
    private String fullName;
    private String designation;
    private Integer serialNumber2025;
    private Integer serialNumber2026;

    public EmployeeSignatureSerialNumberDto(
            Long employeeId,
            String employeeNumber,
            String fullName,
            String designation,
            Integer serialNumber2025,
            Integer serialNumber2026) {

        this.employeeId = employeeId;
        this.employeeNumber = employeeNumber;
        this.fullName = fullName;
        this.designation = designation;
        this.serialNumber2025 = serialNumber2025;
        this.serialNumber2026 = serialNumber2026;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDesignation() {
        return designation;
    }

    public Integer getSerialNumber2025() {
        return serialNumber2025;
    }

    public Integer getSerialNumber2026() {
        return serialNumber2026;
    }
}
