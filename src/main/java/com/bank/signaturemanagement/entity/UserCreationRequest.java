package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_creation_requests")
public class UserCreationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_by")
    private User proposedBy;
    @Column(name = "proposed_username", nullable = false)
    private String proposedUsername;
    @Column(name = "proposed_password_hash", nullable = false)
    private String proposedPasswordHash;
    @Column(name = "proposed_full_name", nullable = false)
    private String proposedFullName;
    @Column(name = "proposed_employee_number", nullable = false)
    private String proposedEmployeeNumber;
    @Column(name = "proposed_email", nullable = false)
    private String proposedEmail;
    @Column(name = "proposed_branch_id", nullable = false)
    private String proposedBranchId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_role_id")
    private Role proposedRole;
    @Column(name = "proposed_scope", nullable = false)
    private String proposedScope = "BOTH";
    @Column(nullable = false)
    private String status = "PENDING_DGM_APPROVAL";
    @Column(name = "rejection_reason")
    private String rejectionReason;
    @Column(name = "dgm_comment")
    private String dgmComment;
    @Column(name = "gm_comment")
    private String gmComment;
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
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public User getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(User v) {
        proposedBy = v;
    }

    public String getProposedUsername() {
        return proposedUsername;
    }

    public void setProposedUsername(String v) {
        proposedUsername = v;
    }

    public String getProposedPasswordHash() {
        return proposedPasswordHash;
    }

    public void setProposedPasswordHash(String v) {
        proposedPasswordHash = v;
    }

    public String getProposedFullName() {
        return proposedFullName;
    }

    public void setProposedFullName(String v) {
        proposedFullName = v;
    }

    public String getProposedEmployeeNumber() {
        return proposedEmployeeNumber;
    }

    public void setProposedEmployeeNumber(String v) {
        proposedEmployeeNumber = v;
    }

    public String getProposedEmail() {
        return proposedEmail;
    }

    public void setProposedEmail(String v) {
        proposedEmail = v;
    }

    public String getProposedBranchId() {
        return proposedBranchId;
    }

    public void setProposedBranchId(String v) {
        proposedBranchId = v;
    }

    public Role getProposedRole() {
        return proposedRole;
    }

    public void setProposedRole(Role v) {
        proposedRole = v;
    }

    public String getProposedScope() {
        return proposedScope;
    }

    public void setProposedScope(String v) {
        proposedScope = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String v) {
        rejectionReason = v;
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

    public User getDgmDecidedBy() { return dgmDecidedBy; }
    public void setDgmDecidedBy(User value) { dgmDecidedBy = value; }
    public User getGmDecidedBy() { return gmDecidedBy; }
    public void setGmDecidedBy(User value) { gmDecidedBy = value; }
    public LocalDateTime getDgmDecidedAt() { return dgmDecidedAt; }
    public void setDgmDecidedAt(LocalDateTime value) { dgmDecidedAt = value; }
    public LocalDateTime getGmDecidedAt() { return gmDecidedAt; }
    public void setGmDecidedAt(LocalDateTime value) { gmDecidedAt = value; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime value) { decidedAt = value; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
}
