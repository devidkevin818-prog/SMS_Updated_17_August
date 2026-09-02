package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature_book_access")
public class SignatureBookAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private SignatureBook book;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by")
    private User grantedBy;
    @Column(name = "granted_at", insertable = false, updatable = false)
    private LocalDateTime grantedAt;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public SignatureBook getBook() {
        return book;
    }

    public void setBook(SignatureBook v) {
        book = v;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User v) {
        user = v;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role v) {
        role = v;
    }

    public User getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(User v) {
        grantedBy = v;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        active = v;
    }
}
