package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.ApprovalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    @EntityGraph(attributePaths = "actedBy")
    Page<ApprovalHistory> findByRequestIdOrderByActionAtDesc(
            Long requestId, Pageable pageable);

    @EntityGraph(attributePaths = {"request", "actedBy"})
    Page<ApprovalHistory> findByActedByUsernameAndApprovalLevelOrderByActionAtDesc(
            String username, String approvalLevel, Pageable pageable);
}
