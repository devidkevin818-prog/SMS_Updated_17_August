package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_change_proposals")
public class EmployeeChangeProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;
    @Column(nullable = false, length = 500)
    private String justification;
    @Column(name = "proposed_data", nullable = false, columnDefinition = "nvarchar(max)")
    private String proposedData = "{}";
    @Column(nullable = false)
    private String status = "PD_ACTION_REQUIRED";
    @Column(name = "pd_comment")
    private String pdComment;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee v) {
        employee = v;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User v) {
        requestedBy = v;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String v) {
        justification = v;
    }

    public String getProposedData() {
        return proposedData;
    }

    public void setProposedData(String v) {
        proposedData = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getPdComment() {
        return pdComment;
    }

    public void setPdComment(String v) {
        pdComment = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }
}
