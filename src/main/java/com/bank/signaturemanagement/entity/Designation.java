package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Designation")
@Getter
@Setter
public class Designation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DesignationId")
    private Long designationId;

    @Column(name = "DesignationName", nullable = false)
    private String designationName;

    @Column(name = "Description")
    private String description;

    @Column(name = "IsActive")
    private Boolean isActive;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}