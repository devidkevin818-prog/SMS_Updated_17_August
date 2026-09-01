package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeStatusRepository
        extends JpaRepository<EmployeeStatus, Long> {

    List<EmployeeStatus> findAllByOrderByStatusIdAsc();
    List<EmployeeStatus> findAllByOrderByDisplayOrderAscStatusNameAsc();
    List<EmployeeStatus> findByActiveTrueOrderByDisplayOrderAscStatusNameAsc();
    boolean existsByStatusNameIgnoreCase(String name);
    boolean existsByStatusNameIgnoreCaseAndStatusIdNot(String name, Long id);
}

