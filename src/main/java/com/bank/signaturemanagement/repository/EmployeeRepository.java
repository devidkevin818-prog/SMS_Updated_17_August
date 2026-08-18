package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumberAndIdNot(String employeeNumber, Long id);
    Page<Employee> findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String employeeNumber, String fullName, Pageable pageable);
}
