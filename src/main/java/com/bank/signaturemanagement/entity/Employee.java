package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;


@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", nullable = false, unique = true, length = 30)
    private String employeeNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "designation",
            referencedColumnName = "DesignationId",
            nullable = false
    )
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department",
            referencedColumnName = "DepartmentId",
            nullable = false
    )
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "branch_code",
            referencedColumnName = "branch_id",
            nullable = false
    )
    private Branch branch;

    @Column(name = "photo_path", nullable = false, length = 500)
    private String photoPath;

    @Column(name = "signature_path", nullable = false, length = 500)
    private String signaturePath;

    @Column(name = "foreign_signature_path", nullable = true,length = 500)
    private String foreignSignaturePath;

    public String getForeignSignaturePath() {
        return foreignSignaturePath;
    }

    public void setForeignSignaturePath(String foreignSignaturePath) {
        this.foreignSignaturePath = foreignSignaturePath;
    }


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "signature_valid_from", nullable = false)
    private LocalDate signatureValidFrom;

    @Column(name = "signature_valid_until", nullable = false)
    private LocalDate signatureValidUntil;


    @Column(name = "update_request_status")
    private Boolean updateRequestStatus = false;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "classification", nullable = false, length = 10)
    private String classification = "BOTH";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private EmployeeStatus employeeStatus;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Boolean getUpdateRequestStatus() { return updateRequestStatus; }
    public void setUpdateRequestStatus(Boolean updateRequestStatus) {
        this.updateRequestStatus = updateRequestStatus;
    }




    public Employee() {
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }


    public Designation getDesignation() {
        return designation;
    }

    public void setDesignation(Designation designation) {
        this.designation = designation;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDate getSignatureValidFrom() {
        return signatureValidFrom;
    }

    public void setSignatureValidFrom(LocalDate signatureValidFrom) {
        this.signatureValidFrom = signatureValidFrom;
    }

    public LocalDate getSignatureValidUntil() {
        return signatureValidUntil;
    }

    public void setSignatureValidUntil(LocalDate signatureValidUntil) {
        this.signatureValidUntil = signatureValidUntil;
    }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public EmployeeStatus getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(EmployeeStatus employeeStatus) { this.employeeStatus = employeeStatus; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

}
