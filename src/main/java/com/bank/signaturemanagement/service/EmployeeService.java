package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }


    // =========================================================
    // SEARCH EMPLOYEE
    // =========================================================

    @Transactional(readOnly = true)
    public Page<Employee> search(String query, int page) {

        String text = query == null
                ? ""
                : query.trim();

        return employeeRepository
                .findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        text,
                        text,
                        PageRequest.of(page, 20)
                );
    }


    // =========================================================
    // GET EMPLOYEE
    // =========================================================

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {

        return employeeRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found"
                        )
                );
    }


    // =========================================================
    // GET UPDATE FORM
    // =========================================================

    @Transactional(readOnly = true)
    public EmployeeUpdateForm getUpdateForm(Long id) {

        Employee employee =
                employeeRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found"
                                )
                        );


        EmployeeUpdateForm form =
                new EmployeeUpdateForm();


        // =====================================================
        // Employee Basic Information
        // =====================================================

        form.setEmployeeCode(
                employee.getEmployeeNumber()
        );


        form.setEmployeeName(
                employee.getFullName()
        );


        // =====================================================
        // Designation
        // Entity -> String
        // =====================================================

        if (employee.getDesignation() != null) {
            form.setDesignationId(employee.getDesignation().getDesignationId());
        }


        // =====================================================
        // Department
        // Entity -> String
        // =====================================================

        if (employee.getDepartment() != null) {
            form.setDepartmentId(employee.getDepartment().getDepartmentId());
        }


        // =====================================================
        // Branch
        // Entity -> String
        // =====================================================

        if (employee.getBranch() != null) {
            form.setBranchId(employee.getBranch().getBranchId());
        }

        // =====================================================
        // Signature Validity
        // =====================================================

        form.setSignatureValidFrom(
                employee.getSignatureValidFrom()
        );


        form.setSignatureValidUntil(
                employee.getSignatureValidUntil()
        );


        return form;
    }
}