package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSerialNumberRepository
        extends JpaRepository<EmployeeSerialNumber, Long> {

    Optional<EmployeeSerialNumber>
    findTopByEmployee_IdOrderByCreatedAtDesc(Long employeeId);

    List<EmployeeSerialNumber>
    findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);

    List<EmployeeSerialNumber>
    findAllByOrderByCreatedAtDesc();

    boolean existsByEmployee_Id(Long employeeId);

    boolean existsByNewLocalSerial(Integer newLocalSerial);

    boolean existsByNewForeignSerial(Integer newForeignSerial);

}