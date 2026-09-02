package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.AuditLog;
import com.bank.signaturemanagement.repository.AuditLogRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository logs;
    private final UserRepository users;

    public AuditService(AuditLogRepository logs, UserRepository users) { this.logs = logs; this.users = users; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String action, String entity, String targetId,
                       String ip, String result, String oldValue, String newValue, String details) {
        AuditLog log = new AuditLog();
        if (username != null && !"anonymousUser".equals(username)) {
            log.setUsername(username);
            users.findByUsername(username).ifPresent(log::setUser);
        }
        log.setActionType(limit(action,80)); log.setTargetEntity(limit(entity,80));
        log.setTargetId(limit(targetId,80)); log.setIpAddress(limit(ip,64)); log.setResult(limit(result,20));
        log.setOldValue(oldValue); log.setNewValue(newValue); log.setDetails(limit(details,1000));
        log.setCorrelationId(UUID.randomUUID().toString());
        logs.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String query, String action, int page) {
        return logs.search(clean(query), clean(action), PageRequest.of(Math.max(0,page), 50));
    }
    @Transactional(readOnly = true)
    public Page<AuditLog> search(String query, String action, int page, int size) {
        return logs.search(clean(query), clean(action), PageRequest.of(Math.max(0,page), Math.min(Math.max(size,1),1000)));
    }
    @Transactional(readOnly = true)
    public Page<AuditLog> relatedTo(String username, int page) {
        return logs.findByUsernameOrderByEventTimeDesc(username, PageRequest.of(Math.max(0,page),50));
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(max,value.length())); }
}
