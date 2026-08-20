package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByOrderByDepartmentNameAsc();

    Optional<Department> findByDepartmentName(String departmentName);

    boolean existsByDepartmentName(String departmentName);
}