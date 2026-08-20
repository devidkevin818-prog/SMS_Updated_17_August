package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "Department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentId")
    private Long departmentId;

    @Column(name = "DepartmentName", nullable = false, length = 100)
    private String departmentName;

    public Department() {
    }

}