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

    @Column(nullable = false, length = 100)
    private String designation;


    @Column(name = "update_request_status", nullable = false)
    private boolean updateRequestStatus = false;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "branch_code", nullable = false, length = 100)
    private String branchCode;

    @Column(name = "photo_path", nullable = false, length = 500)
    private String photoPath;

    @Column(name = "signature_path", nullable = false, length = 500)
    private String signaturePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "signature_valid_from", nullable = false)
    private LocalDate signatureValidFrom;

    @Column(name = "signature_valid_until", nullable = false)
    private LocalDate signatureValidUntil;


    public Employee() {
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
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
    public boolean isUpdateRequestStatus() {
        return updateRequestStatus;
    }

    public void setUpdateRequestStatus(boolean updateRequestStatus) {
        this.updateRequestStatus = updateRequestStatus;
    }

}
