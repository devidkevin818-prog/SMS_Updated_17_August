package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private EmployeeRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acted_by", nullable = false)
    private User actedBy;

    @Column(name = "approval_level", nullable = false, length = 10)
    private String approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalAction action;

    @Column(nullable = false, length = 500)
    private String remark;

    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt = LocalDateTime.now();

    public ApprovalHistory() {
    }

    public Long getId() { return id; }
    public EmployeeRequest getRequest() { return request; }
    public void setRequest(EmployeeRequest request) { this.request = request; }
    public User getActedBy() { return actedBy; }
    public void setActedBy(User actedBy) { this.actedBy = actedBy; }
    public String getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(String approvalLevel) { this.approvalLevel = approvalLevel; }
    public ApprovalAction getAction() { return action; }
    public void setAction(ApprovalAction action) { this.action = action; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getActionAt() { return actionAt; }
}
