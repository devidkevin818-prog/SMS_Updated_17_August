package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.ImportBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImportBatchRepository
        extends JpaRepository<ImportBatch, Long> {

    @EntityGraph(attributePaths = "uploadedBy")
    List<ImportBatch> findAllByActiveTrueAndStatusInOrderByUploadedAtAsc(
            Collection<String> statuses
    );

    @Query("""
            SELECT batch
            FROM ImportBatch batch
            LEFT JOIN FETCH batch.uploadedBy
            WHERE batch.id = :id
              AND batch.active = true
            """)
    Optional<ImportBatch> findActiveByIdWithUploadedBy(
            @Param("id") Long id
    );

    long countByActiveTrueAndStatusIn(
            Collection<String> statuses
    );
}