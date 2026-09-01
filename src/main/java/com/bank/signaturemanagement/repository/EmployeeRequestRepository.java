package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"requestedBy", "designation", "department", "branch", "employeeStatus", "targetEmployee", "changeProposal"})
    Optional<EmployeeRequest> findById(Long id);

    @EntityGraph(attributePaths = {"requestedBy", "designation", "department", "branch", "employeeStatus", "targetEmployee", "changeProposal"})
    Page<EmployeeRequest> findByStatusOrderByRequestedAtAsc(
            RequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"requestedBy", "designation", "department", "branch", "employeeStatus", "targetEmployee", "changeProposal"})
    Page<EmployeeRequest> findByRequestedByIdOrderByRequestedAtDesc(
            Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"targetEmployee"})
    List<EmployeeRequest> findByStatusIn(List<RequestStatus> statuses);

    boolean existsByEmployeeCodeAndStatusIn(String employeeCode, java.util.Collection<RequestStatus> statuses);

    boolean existsByTargetEmployeeIdAndStatusIn(Long employeeId, java.util.Collection<RequestStatus> statuses);
}
