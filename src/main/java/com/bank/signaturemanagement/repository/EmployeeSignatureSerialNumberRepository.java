package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeSignatureSerialNumber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeSignatureSerialNumberRepository
        extends JpaRepository<EmployeeSignatureSerialNumber, Long> {

    @EntityGraph(attributePaths = "employee")
    @Query("""
    SELECT new com.bank.signaturemanagement.dto.EmployeeSignatureSerialNumberDto(
        emp.id,
        emp.employeeNumber,
        emp.fullName,
        emp.designation,
        serial.serialNumber2025,
        serial.serialNumber2026
    )
    FROM Employee emp
    LEFT JOIN EmployeeSignatureSerialNumber serial
        ON serial.employeeId = emp.id
    WHERE
        :query = ''
        OR LOWER(emp.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(emp.employeeNumber) LIKE LOWER(CONCAT('%', :query, '%'))
        OR CAST(emp.id AS string) LIKE CONCAT('%', :query, '%')
    """)
    Page<EmployeeSignatureSerialNumber> findWithEmployee(
            @Param("query") String query,
            Pageable pageable
    );


    Optional<EmployeeSignatureSerialNumber> findByEmployeeId(Long employeeId);

    Optional<EmployeeSignatureSerialNumber> findBySerialNumber2025(
            Integer serialNumber2025
    );

    Optional<EmployeeSignatureSerialNumber> findBySerialNumber2026(
            Integer serialNumber2026
    );


}
