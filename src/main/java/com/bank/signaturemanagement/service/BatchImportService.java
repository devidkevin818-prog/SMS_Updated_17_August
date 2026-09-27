package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.ImportBatch;
import com.bank.signaturemanagement.entity.ImportBatchItem;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.ImportBatchItemRepository;
import com.bank.signaturemanagement.repository.ImportBatchRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BatchImportService {

   // public static final String STATUS_PENDING_DGM = "PENDING_DGM";
    public static final String STATUS_PENDING_LEVEL_1 = "PENDING_LEVEL_1";
    public static final String STATUS_PENDING_LEVEL_2 = "PENDING_LEVEL_2";

    public static final String STATUS_PENDING_GM = "PENDING_GM";
    public static final String STATUS_DGM_REJECTED = "DGM_REJECTED";

    private static final Set<String> LEVEL_1_PENDING_STATUSES = Set.of(
            STATUS_PENDING_LEVEL_2,
            STATUS_PENDING_LEVEL_1
    );

    private final ImportBatchRepository importBatchRepository;
    private final ImportBatchItemRepository importBatchItemRepository;
    private final UserRepository userRepository;

    public BatchImportService(
            ImportBatchRepository importBatchRepository,
            ImportBatchItemRepository importBatchItemRepository,
            UserRepository userRepository
    ) {
        this.importBatchRepository = importBatchRepository;
        this.importBatchItemRepository = importBatchItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ImportBatch> findPendingForLevel1Checker() {
        return importBatchRepository
                .findAllByActiveTrueAndStatusInOrderByUploadedAtAsc(
                        LEVEL_1_PENDING_STATUSES
                );
    }

    /**
     * Backward-compatible DGM naming.
     */
    @Transactional(readOnly = true)
    public List<ImportBatch> findPendingForDgm() {
        return findPendingForLevel1Checker();
    }

    @Transactional(readOnly = true)
    public long countPendingForLevel1Checker() {
        return importBatchRepository.countByActiveTrueAndStatusIn(
                LEVEL_1_PENDING_STATUSES
        );
    }

    @Transactional(readOnly = true)
    public ImportBatch getActiveBatch(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "Batch ID is required."
            );
        }

        return importBatchRepository
                .findActiveByIdWithUploadedBy(batchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Import batch was not found: " + batchId
                ));
    }

    @Transactional(readOnly = true)
    public List<ImportBatchItem> getBatchItems(Long batchId) {
        getActiveBatch(batchId);

        return importBatchItemRepository
                .findAllByBatchIdOrderByRowNumberAsc(batchId);
    }

    /**
     * Primary controller method.
     *
     * The authenticated Level 1 Checker is obtained automatically.
     */
    @Transactional
    public ImportBatch decide(
            Long batchId,
            String action,
            String comment
    ) {
        User currentUser = getAuthenticatedUser();

        return decide(
                batchId,
                action,
                comment,
                currentUser
        );
    }

    /**
     * Backward-compatible method if another controller already passes User.
     */
    @Transactional
    public ImportBatch decide(
            Long batchId,
            String action,
            String comment,
            User currentUser
    ) {
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "Batch ID is required."
            );
        }

        if (currentUser == null) {
            throw new IllegalArgumentException(
                    "The deciding user is required."
            );
        }

        ImportBatch batch = importBatchRepository
                .findActiveByIdWithUploadedBy(batchId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Import batch was not found: " + batchId
                ));

        if (!LEVEL_1_PENDING_STATUSES.contains(batch.getStatus())) {
            throw new IllegalStateException(
                    "Batch is not waiting for Level 1 Checker approval. "
                            + "Current status: "
                            + batch.getStatus()
            );
        }

        String normalizedAction = normalizeAction(action);
        String normalizedComment = normalizeComment(comment);

        if ("REJECT".equals(normalizedAction)
                && normalizedComment == null) {
            throw new IllegalArgumentException(
                    "A rejection reason is required."
            );
        }

        batch.setDgmDecidedBy(currentUser);
        batch.setDgmDecidedAt(LocalDateTime.now());
        batch.setDgmComment(normalizedComment);

        if ("APPROVE".equals(normalizedAction)) {
            batch.setStatus(STATUS_PENDING_GM);
            batch.setRejectionReason(null);
        } else {
            batch.setStatus(STATUS_DGM_REJECTED);
            batch.setRejectionReason(normalizedComment);
        }

        return importBatchRepository.save(batch);
    }

    /**
     * Older method name retained for existing code.
     */
    @Transactional
    public ImportBatch makeLevel1Decision(
            Long batchId,
            String action,
            String comment
    ) {
        return decide(batchId, action, comment);
    }

    /**
     * Older DGM method name retained for existing code.
     */
    @Transactional
    public ImportBatch makeDgmDecision(
            Long batchId,
            String action,
            String comment
    ) {
        return decide(batchId, action, comment);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(
                authentication.getName()
        )) {
            throw new IllegalStateException(
                    "No authenticated user was found."
            );
        }

        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Authenticated application user was not found: "
                                + authentication.getName()
                ));
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Decision action is required."
            );
        }

        String normalizedAction = action
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!"APPROVE".equals(normalizedAction)
                && !"REJECT".equals(normalizedAction)) {
            throw new IllegalArgumentException(
                    "Unsupported decision action: " + action
            );
        }

        return normalizedAction;
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        String normalizedComment = comment.trim();

        if (normalizedComment.length() > 500) {
            throw new IllegalArgumentException(
                    "Comment cannot exceed 500 characters."
            );
        }

        return normalizedComment;
    }
}