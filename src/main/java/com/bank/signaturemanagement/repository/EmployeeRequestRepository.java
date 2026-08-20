package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "requestedBy",
            "designation",
            "department",
            "branch"
    })
    Optional<EmployeeRequest> findById(Long id);


    @EntityGraph(attributePaths = {
            "requestedBy",
            "designation",
            "department",
            "branch"
    })
    Page<EmployeeRequest> findByStatusOrderByRequestedAtAsc(
            RequestStatus status,
            Pageable pageable);


    @EntityGraph(attributePaths = {
            "requestedBy",
            "designation",
            "department",
            "branch"
    })
    Page<EmployeeRequest> findByRequestedByIdOrderByRequestedAtDesc(
            Long userId,
            Pageable pageable);


    boolean existsByEmployeeCodeAndStatusIn(
            String employeeCode,
            Collection<RequestStatus> statuses);

    boolean existsByTargetEmployeeIdAndStatusIn(
            Long employeeId,
            Collection<RequestStatus> statuses);
}