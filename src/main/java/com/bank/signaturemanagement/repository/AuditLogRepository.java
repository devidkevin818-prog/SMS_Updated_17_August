package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("select a from AuditLog a where (:query='' or lower(coalesce(a.username,'')) like lower(concat('%',:query,'%')) or lower(a.targetEntity) like lower(concat('%',:query,'%')) or lower(coalesce(a.targetId,'')) like lower(concat('%',:query,'%'))) and (:action='' or a.actionType=:action) order by a.eventTime desc")
    Page<AuditLog> search(@Param("query") String query, @Param("action") String action, Pageable pageable);

    Page<AuditLog> findByUsernameOrderByEventTimeDesc(String username, Pageable pageable);
}
