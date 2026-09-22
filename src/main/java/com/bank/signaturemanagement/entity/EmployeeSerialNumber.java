package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_serial_number_history",
        schema = "dbo"
)
public class EmployeeSerialNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /*
     * Foreign key:
     * employee_serial_number_history.employee_id
     *     -> employees.id
     *
     * Constraint:
     * FK_employee_serial_history_employees
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "FK_employee_serial_history_employees"
            )
    )
    private Employee employee;

    @Column(name = "new_local_serial")
    private Integer newLocalSerial;

    @Column(name = "old_local_serial")
    private Integer oldLocalSerial;

    @Column(name = "new_foreign_serial")
    private Integer newForeignSerial;

    @Column(name = "old_foreign_serial")
    private Integer oldForeignSerial;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EmployeeSerialNumber() {
    }

    public EmployeeSerialNumber(
            Employee employee,
            Integer newLocalSerial,
            Integer oldLocalSerial,
            Integer newForeignSerial,
            Integer oldForeignSerial
    ) {
        this.employee = employee;
        this.newLocalSerial = newLocalSerial;
        this.oldLocalSerial = oldLocalSerial;
        this.newForeignSerial = newForeignSerial;
        this.oldForeignSerial = oldForeignSerial;
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

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getNewLocalSerial() {
        return newLocalSerial;
    }

    public void setNewLocalSerial(Integer newLocalSerial) {
        this.newLocalSerial = newLocalSerial;
    }

    public Integer getOldLocalSerial() {
        return oldLocalSerial;
    }

    public void setOldLocalSerial(Integer oldLocalSerial) {
        this.oldLocalSerial = oldLocalSerial;
    }

    public Integer getNewForeignSerial() {
        return newForeignSerial;
    }

    public void setNewForeignSerial(Integer newForeignSerial) {
        this.newForeignSerial = newForeignSerial;
    }

    public Integer getOldForeignSerial() {
        return oldForeignSerial;
    }

    public void setOldForeignSerial(Integer oldForeignSerial) {
        this.oldForeignSerial = oldForeignSerial;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Convenience method for retrieving the foreign-key value
     * without exposing it as a separate writable JPA field.
     */
    public Long getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }
}