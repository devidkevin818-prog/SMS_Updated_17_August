package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Department;
import com.bank.signaturemanagement.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAllByOrderByDepartmentNameAsc();
    }
}