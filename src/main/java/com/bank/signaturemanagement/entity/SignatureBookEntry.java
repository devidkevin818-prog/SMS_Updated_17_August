package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "signature_book_entries")
public class SignatureBookEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private SignatureBook book;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(name = "serial_number", nullable = false)
    private int serialNumber;
    @Column(name = "signature_type", nullable = false)
    private String signatureType;
    @Column(name = "signature_path", nullable = false)
    private String signaturePath;

    public Long getId() {
        return id;
    }

    public SignatureBook getBook() {
        return book;
    }

    public void setBook(SignatureBook v) {
        book = v;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee v) {
        employee = v;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int v) {
        serialNumber = v;
    }

    public String getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(String v) {
        signatureType = v;
    }

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String v) {
        signaturePath = v;
    }
}
