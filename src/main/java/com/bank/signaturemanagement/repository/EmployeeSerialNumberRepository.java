package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface EmployeeSerialNumberRepository
        extends JpaRepository<EmployeeSerialNumber, Long> {

    Optional<EmployeeSerialNumber> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByNewSerialNumber(Integer newSerialNumber);
}