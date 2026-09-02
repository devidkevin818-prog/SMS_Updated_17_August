package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(length = 50) private String username;
    @Column(name = "action_type", nullable = false, length = 80) private String actionType;
    @Column(name = "target_entity", nullable = false, length = 80) private String targetEntity;
    @Column(name = "target_id", length = 80) private String targetId;
    @Column(name = "event_time", nullable = false, insertable = false, updatable = false) private LocalDateTime eventTime;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "old_value", columnDefinition = "nvarchar(max)") private String oldValue;
    @Column(name = "new_value", columnDefinition = "nvarchar(max)") private String newValue;
    @Column(nullable = false, length = 20) private String result;
    @Column(name = "correlation_id", length = 80) private String correlationId;
    @Column(length = 1000) private String details;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public LocalDateTime getEventTime() { return eventTime; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
