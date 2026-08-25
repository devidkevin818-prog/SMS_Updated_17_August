package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeStatusRepository
        extends JpaRepository<EmployeeStatus, Integer> {

    List<EmployeeStatus> findAllByOrderByStatusIdAsc();
}

