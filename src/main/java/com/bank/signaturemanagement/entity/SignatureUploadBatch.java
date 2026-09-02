package com.bank.signaturemanagement.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="signature_upload_batches") public class SignatureUploadBatch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="batch_number",nullable=false,unique=true) private String batchNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="submitted_by") private User submittedBy;
 @Column(name="total_files",nullable=false) private int totalFiles; @Column(name="matched_files",nullable=false) private int matchedFiles; @Column(name="invalid_files",nullable=false) private int invalidFiles;
 @Column(nullable=false) private String status="DRAFT"; @Column(name="created_at",insertable=false,updatable=false) private LocalDateTime createdAt;
 public Long getId(){return id;} public String getBatchNumber(){return batchNumber;} public void setBatchNumber(String v){batchNumber=v;} public User getSubmittedBy(){return submittedBy;} public void setSubmittedBy(User v){submittedBy=v;} public int getTotalFiles(){return totalFiles;} public void setTotalFiles(int v){totalFiles=v;} public int getMatchedFiles(){return matchedFiles;} public void setMatchedFiles(int v){matchedFiles=v;} public int getInvalidFiles(){return invalidFiles;} public void setInvalidFiles(int v){invalidFiles=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}
