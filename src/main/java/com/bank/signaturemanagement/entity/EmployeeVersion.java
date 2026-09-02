package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_versions")
public class EmployeeVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(name = "version_no", nullable = false)
    private int versionNo;
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "nvarchar(max)")
    private String snapshotJson;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee v) {
        employee = v;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int v) {
        versionNo = v;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String v) {
        snapshotJson = v;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(User v) {
        changedBy = v;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String v) {
        reason = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
