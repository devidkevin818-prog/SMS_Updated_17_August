package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_requests")
public class EmployeeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_employee_id")
    private Employee targetEmployee;
    @Column(name = "employee_code", nullable = false, length = 30)
    private String employeeCode;
    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(name = "update_request_status", nullable = false)
    private boolean updateRequestStatus = false;

    @Column(name = "updated_after_rejection", nullable = false)
    private boolean updatedAfterRejection = false;

    public boolean isUpdateRequestStatus() { return updateRequestStatus; }
    public void setUpdateRequestStatus(boolean updateRequestStatus) { this.updateRequestStatus = updateRequestStatus; }

    public boolean isUpdatedAfterRejection() { return updatedAfterRejection; }
    public void setUpdatedAfterRejection(boolean updatedAfterRejection) { this.updatedAfterRejection = updatedAfterRejection; }

    // @Column(nullable = false, length = 100)
    // private String designation;

    // @Column(nullable = false, length = 100)
    // private String department;

    // @Column(nullable = false, length = 100)
    // private String branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "designation",
            referencedColumnName = "DesignationId"
    )
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "department",
            referencedColumnName = "DepartmentId"
    )
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "branch",
            referencedColumnName = "branch_id"
    )
    private Branch branch;


    @Column(name = "photo_path", nullable = false, length = 500)
    private String photoPath;
    @Column(name = "signature_path", nullable = false, length = 500)
    private String signaturePath;
    @Column(name = "signature_valid_from", nullable = false)
    private LocalDate signatureValidFrom;
    @Column(name = "signature_valid_until", nullable = false)
    private LocalDate signatureValidUntil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.PENDING_DGM;
    @Column(nullable = false, length = 500)
    private String remark;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public EmployeeRequest() {
    }

    public Long getId() {
        return id;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Employee getTargetEmployee() {
        return targetEmployee;
    }

    public void setTargetEmployee(Employee targetEmployee) {
        this.targetEmployee = targetEmployee;
    }

    public boolean isUpdateRequest() {
        return targetEmployee != null;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

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

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String signaturePath) {
        this.signaturePath = signaturePath;
    }

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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}