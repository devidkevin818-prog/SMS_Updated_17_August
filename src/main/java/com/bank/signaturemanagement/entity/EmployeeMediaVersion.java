package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_media_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_employee_media_version", columnNames = {"employee_id", "version_number"}))
public class EmployeeMediaVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private EmployeeRequest request;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "signature_path", length = 500)
    private String signaturePath;

    @Column(name = "foreign_signature_path", nullable = true,length = 500)
    private String foreignSignaturePath;

    public String getForeignSignaturePath() {
        return foreignSignaturePath;
    }

    public void setForeignSignaturePath(String foreignSignaturePath) {
        this.foreignSignaturePath = foreignSignaturePath;
    }


    @Column(name = "approved_at", nullable = false, updatable = false)
    private LocalDateTime approvedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public EmployeeRequest getRequest() { return request; }
    public void setRequest(EmployeeRequest request) { this.request = request; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
}
