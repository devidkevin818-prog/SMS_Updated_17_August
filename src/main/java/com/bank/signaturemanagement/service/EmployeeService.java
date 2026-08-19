package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> search(String query, int page) {
        String text = query == null ? "" : query.trim();
        return employeeRepository.findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                text, text, PageRequest.of(page, 20));
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    @Transactional(readOnly = true)
    public EmployeeUpdateForm getUpdateForm(Long id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        EmployeeUpdateForm form = new EmployeeUpdateForm();

        form.setEmployeeCode(employee.getEmployeeNumber());
        form.setEmployeeName(employee.getFullName());
        form.setDesignation(employee.getDesignation());
        form.setDepartment(employee.getDepartment());
        form.setBranch(employee.getBranchCode());

        form.setSignatureValidFrom(employee.getSignatureValidFrom());
        form.setSignatureValidUntil(employee.getSignatureValidUntil());

        return form;
    }
    @Transactional
    public void requestUpdate(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        employee.setUpdateRequestStatus(true);
        employeeRepository.save(employee);
    }

    @Transactional
    public void updateRequestStatus(Long id, boolean status) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        employee.setUpdateRequestStatus(status);
        employeeRepository.save(employee);
    }





}
