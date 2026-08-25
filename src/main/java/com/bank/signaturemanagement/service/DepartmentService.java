package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Department;
import com.bank.signaturemanagement.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByDepartmentNameAsc();
    }
}
