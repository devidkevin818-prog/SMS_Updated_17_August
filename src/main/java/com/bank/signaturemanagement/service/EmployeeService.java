package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.Employee;
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

    @Transactional(readOnly = true)
    public Page<Employee> search(String query, int page) {
        String text = query == null ? "" : query.trim();
        return employeeRepository.findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                text, text, PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public Page<Employee> filter(String query, Long departmentId, Long designationId, Long branchId, int page) {
        String text = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        return employeeRepository.filter(text, departmentId, designationId, branchId, PageRequest.of(safePage, 20));
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    @Transactional(readOnly = true)
    public EmployeeUpdateForm getUpdateForm(Long id) {
        Employee employee = getEmployee(id);
        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setEmployeeCode(EmployeeNumberFormat.editablePart(employee.getEmployeeNumber()));
        form.setEmployeeName(employee.getFullName());
        if (employee.getDesignation() != null) {
            form.setDesignationId(employee.getDesignation().getDesignationId());
        }
        if (employee.getDepartment() != null) {
            form.setDepartmentId(employee.getDepartment().getDepartmentId());
        }
        if (employee.getBranch() != null) {
            form.setBranchId(employee.getBranch().getBranchId());
        }
        form.setSignatureValidFrom(employee.getSignatureValidFrom());
        form.setSignatureValidUntil(employee.getSignatureValidUntil());
        form.setStatusId(employee.getEmployeeStatus()==null?null:employee.getEmployeeStatus().getStatusId());
        form.setClassification(employee.getClassification());
        form.setJoiningDate(employee.getJoiningDate());
        return form;
    }

    @Transactional
    public void requestUpdate(Long id) {
        Employee employee = getEmployee(id);
        employee.setUpdateRequestStatus(true);
        employeeRepository.save(employee);
    }

    @Transactional
    public void updateRequestStatus(Long id, boolean status) {
        Employee employee = getEmployee(id);
        employee.setUpdateRequestStatus(status);
        employeeRepository.save(employee);
    }
}
