package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
public class ImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "batch_number", nullable = false, unique = true, length = 40)
    private String batchNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    @Column(name = "original_file_path")
    private String originalFilePath;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "total_rows", nullable = false)
    private int totalRows;
    @Column(name = "succeeded_rows", nullable = false)
    private int succeededRows;
    @Column(name = "failed_rows", nullable = false)
    private int failedRows;
    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private LocalDateTime uploadedAt;
    @Column(name = "dgm_comment")
    private String dgmComment;
    @Column(name = "gm_comment")
    private String gmComment;
    @Column(name = "rejection_reason")
    private String rejectionReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dgm_decided_by")
    private User dgmDecidedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gm_decided_by")
    private User gmDecidedBy;
    @Column(name = "dgm_decided_at")
    private LocalDateTime dgmDecidedAt;
    @Column(name = "gm_decided_at")
    private LocalDateTime gmDecidedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_of_batch_id")
    private ImportBatch retryOf;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String v) {
        batchNumber = v;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User v) {
        uploadedBy = v;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String v) {
        originalFilename = v;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String v) {
        originalFilePath = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int v) {
        totalRows = v;
    }

    public int getSucceededRows() {
        return succeededRows;
    }

    public void setSucceededRows(int v) {
        succeededRows = v;
    }

    public int getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(int v) {
        failedRows = v;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getDgmComment() {
        return dgmComment;
    }

    public void setDgmComment(String v) {
        dgmComment = v;
    }

    public String getGmComment() {
        return gmComment;
    }

    public void setGmComment(String v) {
        gmComment = v;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String v) {
        rejectionReason = v;
    }

    public User getDgmDecidedBy() { return dgmDecidedBy; }
    public void setDgmDecidedBy(User value) { dgmDecidedBy = value; }
    public User getGmDecidedBy() { return gmDecidedBy; }
    public void setGmDecidedBy(User value) { gmDecidedBy = value; }
    public LocalDateTime getDgmDecidedAt() { return dgmDecidedAt; }
    public void setDgmDecidedAt(LocalDateTime value) { dgmDecidedAt = value; }
    public LocalDateTime getGmDecidedAt() { return gmDecidedAt; }
    public void setGmDecidedAt(LocalDateTime value) { gmDecidedAt = value; }

    public ImportBatch getRetryOf() {
        return retryOf;
    }

    public void setRetryOf(ImportBatch v) {
        retryOf = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
}
