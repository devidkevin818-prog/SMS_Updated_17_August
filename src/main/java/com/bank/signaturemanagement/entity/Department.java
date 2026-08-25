package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;
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

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

}
