package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_creation_requests")
public class UserCreationRequest {

    public static final String STATUS_PENDING_LEVEL_1_CHECKER_APPROVAL =
            "PENDING_LEVEL_1_CHECKER_APPROVAL";

    public static final String STATUS_PENDING_LEVEL_2_CHECKER_APPROVAL =
            "PENDING_LEVEL_2_CHECKER_APPROVAL";

    public static final String STATUS_APPROVED =
            "APPROVED";

    public static final String STATUS_REJECTED =
            "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "proposed_by",
            nullable = false
    )
    private User proposedBy;

    @Column(
            name = "proposed_username",
            nullable = false,
            length = 50
    )
    private String proposedUsername;

    @Column(
            name = "proposed_password_hash",
            nullable = false,
            length = 255
    )
    private String proposedPasswordHash;

    @Column(
            name = "proposed_full_name",
            nullable = false,
            length = 100
    )
    private String proposedFullName;

    @Column(
            name = "proposed_employee_number",
            nullable = false,
            length = 30
    )
    private String proposedEmployeeNumber;

    @Column(
            name = "proposed_email",
            nullable = false,
            length = 100
    )
    private String proposedEmail;

    /*
     * Java uses Long consistently for branch IDs.
     *
     * The SQL Server column should preferably be BIGINT. If the existing
     * database column is VARCHAR but contains numeric values, Hibernate and
     * SQL Server may convert it, but migrating the column to BIGINT is the
     * recommended permanent solution.
     */
    @Column(
            name = "proposed_branch_id",
            nullable = false
    )
    private Long proposedBranchId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "proposed_role_id",
            nullable = false
    )
    private Role proposedRole;

    @Column(
            name = "proposed_scope",
            nullable = false,
            length = 10
    )
    private String proposedScope = "BOTH";

    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private String status =
            STATUS_PENDING_LEVEL_1_CHECKER_APPROVAL;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    /*
     * New Java business name mapped to the existing DGM database column.
     */
    @Column(
            name = "dgm_comment",
            length = 500
    )
    private String level1CheckerComment;

    /*
     * New Java business name mapped to the existing GM database column.
     */
    @Column(
            name = "gm_comment",
            length = 500
    )
    private String level2CheckerComment;

    /*
     * Level 1 Checker user mapped to the existing dgm_decided_by column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dgm_decided_by")
    private User level1CheckerDecidedBy;

    /*
     * Level 2 Checker user mapped to the existing gm_decided_by column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gm_decided_by")
    private User level2CheckerDecidedBy;

    /*
     * Level 1 Checker decision time mapped to dgm_decided_at.
     */
    @Column(name = "dgm_decided_at")
    private LocalDateTime level1CheckerDecidedAt;

    /*
     * Level 2 Checker decision time mapped to gm_decided_at.
     */
    @Column(name = "gm_decided_at")
    private LocalDateTime level2CheckerDecidedAt;

    /*
     * Overall final decision time.
     */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /*
     * The database is responsible for generating created_at.
     */
    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public User getProposedBy() {
        return proposedBy;
    }

    public void setProposedBy(User proposedBy) {
        this.proposedBy = proposedBy;
    }

    public String getProposedUsername() {
        return proposedUsername;
    }

    public void setProposedUsername(
            String proposedUsername
    ) {
        this.proposedUsername = proposedUsername;
    }

    public String getProposedPasswordHash() {
        return proposedPasswordHash;
    }

    public void setProposedPasswordHash(
            String proposedPasswordHash
    ) {
        this.proposedPasswordHash = proposedPasswordHash;
    }

    public String getProposedFullName() {
        return proposedFullName;
    }

    public void setProposedFullName(
            String proposedFullName
    ) {
        this.proposedFullName = proposedFullName;
    }

    public String getProposedEmployeeNumber() {
        return proposedEmployeeNumber;
    }

    public void setProposedEmployeeNumber(
            String proposedEmployeeNumber
    ) {
        this.proposedEmployeeNumber =
                proposedEmployeeNumber;
    }

    public String getProposedEmail() {
        return proposedEmail;
    }

    public void setProposedEmail(
            String proposedEmail
    ) {
        this.proposedEmail = proposedEmail;
    }

    public Long getProposedBranchId() {
        return proposedBranchId;
    }

    /*
     * Keep only this one setter.
     */
    public void setProposedBranchId(
            Long proposedBranchId
    ) {
        this.proposedBranchId = proposedBranchId;
    }

    public Role getProposedRole() {
        return proposedRole;
    }

    public void setProposedRole(
            Role proposedRole
    ) {
        this.proposedRole = proposedRole;
    }

    public String getProposedScope() {
        return proposedScope;
    }

    public void setProposedScope(
            String proposedScope
    ) {
        this.proposedScope = proposedScope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(
            String rejectionReason
    ) {
        this.rejectionReason = rejectionReason;
    }

    public String getLevel1CheckerComment() {
        return level1CheckerComment;
    }

    public void setLevel1CheckerComment(
            String level1CheckerComment
    ) {
        this.level1CheckerComment =
                level1CheckerComment;
    }

    public String getLevel2CheckerComment() {
        return level2CheckerComment;
    }

    public void setLevel2CheckerComment(
            String level2CheckerComment
    ) {
        this.level2CheckerComment =
                level2CheckerComment;
    }

    public User getLevel1CheckerDecidedBy() {
        return level1CheckerDecidedBy;
    }

    public void setLevel1CheckerDecidedBy(
            User level1CheckerDecidedBy
    ) {
        this.level1CheckerDecidedBy =
                level1CheckerDecidedBy;
    }

    public User getLevel2CheckerDecidedBy() {
        return level2CheckerDecidedBy;
    }

    public void setLevel2CheckerDecidedBy(
            User level2CheckerDecidedBy
    ) {
        this.level2CheckerDecidedBy =
                level2CheckerDecidedBy;
    }

    public LocalDateTime getLevel1CheckerDecidedAt() {
        return level1CheckerDecidedAt;
    }

    public void setLevel1CheckerDecidedAt(
            LocalDateTime level1CheckerDecidedAt
    ) {
        this.level1CheckerDecidedAt =
                level1CheckerDecidedAt;
    }

    public LocalDateTime getLevel2CheckerDecidedAt() {
        return level2CheckerDecidedAt;
    }

    public void setLevel2CheckerDecidedAt(
            LocalDateTime level2CheckerDecidedAt
    ) {
        this.level2CheckerDecidedAt =
                level2CheckerDecidedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(
            LocalDateTime decidedAt
    ) {
        this.decidedAt = decidedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active = active;
    }

    /*
     * Backward-compatible DGM and GM method names.
     *
     * These are Java compatibility methods only. They do not create
     * additional persistent entity properties because field-based JPA
     * access is being used.
     */

    @Deprecated
    public String getDgmComment() {
        return level1CheckerComment;
    }

    @Deprecated
    public void setDgmComment(
            String dgmComment
    ) {
        this.level1CheckerComment = dgmComment;
    }

    @Deprecated
    public String getGmComment() {
        return level2CheckerComment;
    }

    @Deprecated
    public void setGmComment(
            String gmComment
    ) {
        this.level2CheckerComment = gmComment;
    }

    @Deprecated
    public User getDgmDecidedBy() {
        return level1CheckerDecidedBy;
    }

    @Deprecated
    public void setDgmDecidedBy(
            User dgmDecidedBy
    ) {
        this.level1CheckerDecidedBy = dgmDecidedBy;
    }

    @Deprecated
    public User getGmDecidedBy() {
        return level2CheckerDecidedBy;
    }

    @Deprecated
    public void setGmDecidedBy(
            User gmDecidedBy
    ) {
        this.level2CheckerDecidedBy = gmDecidedBy;
    }

    @Deprecated
    public LocalDateTime getDgmDecidedAt() {
        return level1CheckerDecidedAt;
    }

    @Deprecated
    public void setDgmDecidedAt(
            LocalDateTime dgmDecidedAt
    ) {
        this.level1CheckerDecidedAt = dgmDecidedAt;
    }

    @Deprecated
    public LocalDateTime getGmDecidedAt() {
        return level2CheckerDecidedAt;
    }

    @Deprecated
    public void setGmDecidedAt(
            LocalDateTime gmDecidedAt
    ) {
        this.level2CheckerDecidedAt = gmDecidedAt;
    }
}