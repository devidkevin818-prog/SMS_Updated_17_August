package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.ReferenceDataForm;
import com.bank.signaturemanagement.entity.Department;
import com.bank.signaturemanagement.entity.Designation;
import com.bank.signaturemanagement.entity.Branch;
import com.bank.signaturemanagement.entity.EmployeeStatus;
import com.bank.signaturemanagement.repository.DepartmentRepository;
import com.bank.signaturemanagement.repository.DesignationRepository;
import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.EmployeeStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReferenceDataAdminService {
    private final DepartmentRepository departments;
    private final DesignationRepository designations;
    private final BranchRepository branches;
    private final EmployeeStatusRepository statuses;

    public ReferenceDataAdminService(DepartmentRepository departments,
                                     DesignationRepository designations, BranchRepository branches,
                                     EmployeeStatusRepository statuses) {
        this.departments = departments;
        this.designations = designations;
        this.branches = branches;
        this.statuses = statuses;
    }

    @Transactional(readOnly = true)
    public List<Department> departments() {
        return departments.findAllByOrderByDepartmentNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Designation> designations() {
        return designations.findAllByOrderByDesignationNameAsc();
    }
    @Transactional(readOnly = true) public List<Branch> branches() { return branches.findAllByOrderByBranchNameAsc(); }
    @Transactional(readOnly = true) public List<EmployeeStatus> statuses() { return statuses.findAllByOrderByDisplayOrderAscStatusNameAsc(); }

    @Transactional public void createBranch(ReferenceDataForm form) {
        String name=normalizeName(form.getName());
        if(branches.existsByBranchNameIgnoreCase(name)) throw new IllegalArgumentException("Branch already exists");
        Branch value=new Branch(); value.setBranchName(name); value.setDescription(normalizeDescription(form.getDescription())); value.setActive(true); branches.save(value);
    }
    @Transactional public void updateBranch(Long id, ReferenceDataForm form) {
        Branch value=branches.findById(id).orElseThrow(()->new IllegalArgumentException("Branch not found")); String name=normalizeName(form.getName());
        if(branches.existsByBranchNameIgnoreCaseAndBranchIdNot(name,id)) throw new IllegalArgumentException("Branch already exists");
        value.setBranchName(name); value.setDescription(normalizeDescription(form.getDescription()));
    }
    @Transactional public void toggleBranch(Long id) { Branch value=branches.findById(id).orElseThrow(()->new IllegalArgumentException("Branch not found")); value.setActive(!value.isActive()); }

    @Transactional public void createStatus(ReferenceDataForm form) {
        String name=normalizeName(form.getName()); if(statuses.existsByStatusNameIgnoreCase(name)) throw new IllegalArgumentException("Employee status already exists");
        EmployeeStatus value=new EmployeeStatus(); value.setStatusName(name); value.setDisplayOrder(form.getDisplayOrder()==null?100:form.getDisplayOrder()); value.setActive(true); statuses.save(value);
    }
    @Transactional public void updateStatus(Long id, ReferenceDataForm form) {
        EmployeeStatus value=statuses.findById(id).orElseThrow(()->new IllegalArgumentException("Employee status not found")); String name=normalizeName(form.getName());
        if(statuses.existsByStatusNameIgnoreCaseAndStatusIdNot(name,id)) throw new IllegalArgumentException("Employee status already exists");
        value.setStatusName(name); value.setDisplayOrder(form.getDisplayOrder()==null?100:form.getDisplayOrder());
    }
    @Transactional public void toggleStatus(Long id) { EmployeeStatus value=statuses.findById(id).orElseThrow(()->new IllegalArgumentException("Employee status not found")); value.setActive(!value.isActive()); }

    @Transactional
    public void createDepartment(ReferenceDataForm form) {
        String name = normalizeName(form.getName());
        if (departments.existsByDepartmentNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Department already exists");
        }
        Department department = new Department();
        department.setDepartmentName(name);
        department.setDescription(normalizeDescription(form.getDescription()));
        department.setActive(true);
        departments.save(department);
    }

    @Transactional
    public void updateDepartment(Long id, ReferenceDataForm form) {
        Department department = departments.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        String name = normalizeName(form.getName());
        if (departments.existsByDepartmentNameIgnoreCaseAndDepartmentIdNot(name, id)) {
            throw new IllegalArgumentException("Department already exists");
        }
        department.setDepartmentName(name);
        department.setDescription(normalizeDescription(form.getDescription()));
    }

    @Transactional
    public void toggleDepartment(Long id) {
        Department department = departments.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        department.setActive(!Boolean.TRUE.equals(department.getActive()));
    }

    @Transactional
    public void createDesignation(ReferenceDataForm form) {
        String name = normalizeName(form.getName());
        if (designations.existsByDesignationNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Designation already exists");
        }
        Designation designation = new Designation();
        designation.setDesignationName(name);
        designation.setDescription(normalizeDescription(form.getDescription()));
        designation.setIsActive(true);
        designation.setCreatedAt(LocalDateTime.now());
        designation.setHierarchyOrder(form.getDisplayOrder()==null?100:form.getDisplayOrder());
        designations.save(designation);
    }

    @Transactional
    public void updateDesignation(Long id, ReferenceDataForm form) {
        Designation designation = designations.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));
        String name = normalizeName(form.getName());
        if (designations.existsByDesignationNameIgnoreCaseAndDesignationIdNot(name, id)) {
            throw new IllegalArgumentException("Designation already exists");
        }
        designation.setDesignationName(name);
        designation.setDescription(normalizeDescription(form.getDescription()));
        designation.setHierarchyOrder(form.getDisplayOrder()==null?100:form.getDisplayOrder());
    }

    @Transactional
    public void toggleDesignation(Long id) {
        Designation designation = designations.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));
        designation.setIsActive(!Boolean.TRUE.equals(designation.getIsActive()));
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Name is required");
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDescription(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
