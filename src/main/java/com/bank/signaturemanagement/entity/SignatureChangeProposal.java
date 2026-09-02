package com.bank.signaturemanagement.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="signature_change_proposals") public class SignatureChangeProposal {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private Employee employee;
 @Column(name="signature_type",nullable=false,length=10) private String signatureType;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="initiated_by") private User initiatedBy;
 @Column(name="initiator_remarks",nullable=false,length=500) private String initiatorRemarks;
 @Column(nullable=false,length=30) private String status="PD_ACTION_REQUIRED";
 @Column(name="created_at",insertable=false,updatable=false) private LocalDateTime createdAt;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="submitted_version_id") private SignatureVersion submittedVersion;
 public Long getId(){return id;} public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;} public String getSignatureType(){return signatureType;} public void setSignatureType(String v){signatureType=v;} public User getInitiatedBy(){return initiatedBy;} public void setInitiatedBy(User v){initiatedBy=v;} public String getInitiatorRemarks(){return initiatorRemarks;} public void setInitiatorRemarks(String v){initiatorRemarks=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;} public SignatureVersion getSubmittedVersion(){return submittedVersion;} public void setSubmittedVersion(SignatureVersion v){submittedVersion=v;}
}
