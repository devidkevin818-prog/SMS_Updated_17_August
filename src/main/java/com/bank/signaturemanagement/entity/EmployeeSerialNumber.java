package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "employee_serial_number_history",
        schema = "dbo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_employee_serial_employee_id",
                        columnNames = "employee_id"
                )
        }
)
public class EmployeeSerialNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "new_serial_number", nullable = false)
    private Integer newSerialNumber;

    @Column(name = "old_serial_number")
    private Integer oldSerialNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmployeeSerialNumber() {
    }

    public EmployeeSerialNumber(
            Long employeeId,
            Integer newSerialNumber,
            Integer oldSerialNumber) {

        this.employeeId = employeeId;
        this.newSerialNumber = newSerialNumber;
        this.oldSerialNumber = oldSerialNumber;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getNewSerialNumber() {
        return newSerialNumber;
    }

    public void setNewSerialNumber(Integer newSerialNumber) {
        this.newSerialNumber = newSerialNumber;
    }

    public Integer getOldSerialNumber() {
        return oldSerialNumber;
    }

    public void setOldSerialNumber(Integer oldSerialNumber) {
        this.oldSerialNumber = oldSerialNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}