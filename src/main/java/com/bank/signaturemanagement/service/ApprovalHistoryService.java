package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.ApprovalHistory;
import com.bank.signaturemanagement.repository.ApprovalHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalHistoryService {
    private final ApprovalHistoryRepository approvalHistoryRepository;

    public ApprovalHistoryService(ApprovalHistoryRepository approvalHistoryRepository) {
        this.approvalHistoryRepository = approvalHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ApprovalHistory> getDecisions(String username, String approvalLevel, int page) {
        return approvalHistoryRepository.findByActedByUsernameAndApprovalLevelOrderByActionAtDesc(
                username, approvalLevel, PageRequest.of(page, 20));
    }
}
