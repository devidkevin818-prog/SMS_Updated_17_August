package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.EmployeeChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface EmployeeChangeProposalRepository extends JpaRepository<EmployeeChangeProposal, Long> {
    @EntityGraph(attributePaths = {"employee", "requestedBy"})
    List<EmployeeChangeProposal> findByStatusOrderByCreatedAtAsc(String status);

    boolean existsByEmployeeIdAndStatus(Long employeeId, String status);

    boolean existsByEmployeeIdAndStatusIn(Long employeeId, List<String> statuses);
}
