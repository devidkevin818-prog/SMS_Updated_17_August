package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeMediaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeMediaVersionRepository extends JpaRepository<EmployeeMediaVersion, Long> {
    List<EmployeeMediaVersion> findByEmployeeIdOrderByVersionNumberDesc(Long employeeId);
    long countByEmployeeId(Long employeeId);
}
