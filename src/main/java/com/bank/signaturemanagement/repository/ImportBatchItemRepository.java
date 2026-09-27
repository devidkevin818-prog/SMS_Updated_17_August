package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.ImportBatchItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportBatchItemRepository
        extends JpaRepository<ImportBatchItem, Long> {

    @EntityGraph(attributePaths = {
            "employee"
    })
    List<ImportBatchItem> findAllByBatchIdOrderByRowNumberAsc(
            Long batchId
    );

    long countByBatchId(Long batchId);

    long countByBatchIdAndStatusIgnoreCase(
            Long batchId,
            String status
    );
}