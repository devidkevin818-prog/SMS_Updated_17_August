package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature_books")
public class SignatureBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "book_number", nullable = false, unique = true)
    private String bookNumber;
    @Column(name = "book_year", nullable = false)
    private int bookYear;
    @Column(name = "version_no", nullable = false)
    private int versionNo;
    @Column(name = "signature_type", nullable = false)
    private String signatureType;
    @Column(name = "file_path", nullable = false)
    private String filePath;
    @Column(name = "file_sha256")
    private String fileSha256;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by")
    private User generatedBy;
    @Column(name = "generated_at", insertable = false, updatable = false)
    private LocalDateTime generatedAt;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable=false,length=20) private String status="CURRENT";

    public Long getId() {
        return id;
    }

    public String getBookNumber() {
        return bookNumber;
    }

    public void setBookNumber(String v) {
        bookNumber = v;
    }

    public int getBookYear() {
        return bookYear;
    }

    public void setBookYear(int v) {
        bookYear = v;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int v) {
        versionNo = v;
    }

    public String getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(String v) {
        signatureType = v;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String v) {
        filePath = v;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String v) {
        fileSha256 = v;
    }

    public User getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(User v) {
        generatedBy = v;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
