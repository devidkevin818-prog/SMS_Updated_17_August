package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="signature_versions")
public class SignatureVersion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="employee_id") private Employee employee;
 @Column(name="employee_number",nullable=false,length=30) private String employeeNumber;
 @Column(name="signature_type",nullable=false,length=10) private String signatureType;
 @Column(name="version_number",nullable=false) private int versionNumber;
 @Column(name="file_path",nullable=false,length=500) private String filePath;
 @Column(nullable=false,length=30) private String status="PENDING_DGM";
 @Column(name="current_approved",nullable=false) private boolean currentApproved;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="submitted_by") private User submittedBy;
 @Column(name="submitted_at",insertable=false,updatable=false) private LocalDateTime submittedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="batch_id") private SignatureUploadBatch batch;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="change_proposal_id") private SignatureChangeProposal changeProposal;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="dgm_approver") private User dgmApprover;
 @Column(name="dgm_decided_at") private LocalDateTime dgmDecidedAt;
 @Column(name="dgm_remarks") private String dgmRemarks;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="gm_approver") private User gmApprover;
 @Column(name="gm_decided_at") private LocalDateTime gmDecidedAt;
 @Column(name="gm_remarks") private String gmRemarks;
 @Column(name="rejection_remarks") private String rejectionRemarks;
 public Long getId(){return id;} public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;}
 public String getEmployeeNumber(){return employeeNumber;} public void setEmployeeNumber(String v){employeeNumber=v;}
 public String getSignatureType(){return signatureType;} public void setSignatureType(String v){signatureType=v;}
 public int getVersionNumber(){return versionNumber;} public void setVersionNumber(int v){versionNumber=v;}
 public String getFilePath(){return filePath;} public void setFilePath(String v){filePath=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;} public boolean isCurrentApproved(){return currentApproved;} public void setCurrentApproved(boolean v){currentApproved=v;}
 public User getSubmittedBy(){return submittedBy;} public void setSubmittedBy(User v){submittedBy=v;} public LocalDateTime getSubmittedAt(){return submittedAt;}
 public SignatureUploadBatch getBatch(){return batch;} public void setBatch(SignatureUploadBatch v){batch=v;}
 public SignatureChangeProposal getChangeProposal(){return changeProposal;} public void setChangeProposal(SignatureChangeProposal v){changeProposal=v;}
 public User getDgmApprover(){return dgmApprover;} public void setDgmApprover(User v){dgmApprover=v;} public LocalDateTime getDgmDecidedAt(){return dgmDecidedAt;} public void setDgmDecidedAt(LocalDateTime v){dgmDecidedAt=v;} public String getDgmRemarks(){return dgmRemarks;} public void setDgmRemarks(String v){dgmRemarks=v;}
 public User getGmApprover(){return gmApprover;} public void setGmApprover(User v){gmApprover=v;} public LocalDateTime getGmDecidedAt(){return gmDecidedAt;} public void setGmDecidedAt(LocalDateTime v){gmDecidedAt=v;} public String getGmRemarks(){return gmRemarks;} public void setGmRemarks(String v){gmRemarks=v;} public String getRejectionRemarks(){return rejectionRemarks;} public void setRejectionRemarks(String v){rejectionRemarks=v;}
}
