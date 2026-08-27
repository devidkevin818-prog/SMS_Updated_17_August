package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "employee_signature_serial_numbers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_signature_serial_2025",
                        columnNames = "serial_number_2025"
                ),
                @UniqueConstraint(
                        name = "uk_signature_serial_2026",
                        columnNames = "serial_number_2026"
                )
        }
)
public class EmployeeSignatureSerialNumber {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "serial_number_2025")
    private Integer serialNumber2025;

    @Column(name = "serial_number_2026")
    private Integer serialNumber2026;

    public EmployeeSignatureSerialNumber() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getSerialNumber2025() {
        return serialNumber2025;
    }

    public void setSerialNumber2025(Integer serialNumber2025) {
        this.serialNumber2025 = serialNumber2025;
    }

    public Integer getSerialNumber2026() {
        return serialNumber2026;
    }

    public void setSerialNumber2026(Integer serialNumber2026) {
        this.serialNumber2026 = serialNumber2026;
    }
}
