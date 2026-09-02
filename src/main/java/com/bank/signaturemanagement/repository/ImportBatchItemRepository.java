package com.bank.signaturemanagement.repository;
import com.bank.signaturemanagement.entity.ImportBatchItem; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ImportBatchItemRepository extends JpaRepository<ImportBatchItem,Long>{List<ImportBatchItem> findByBatchIdOrderByRowNumber(Long batchId);List<ImportBatchItem> findByBatchIdAndStatusOrderByRowNumber(Long batchId,String status);java.util.Optional<ImportBatchItem> findByIdAndBatchId(Long id,Long batchId);}
