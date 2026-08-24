package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.*;
import com.bank.signaturemanagement.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeRequestService {
    private final EmployeeRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalHistoryRepository approvalRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;

    public EmployeeRequestService(EmployeeRequestRepository requestRepository,
                                  EmployeeRepository employeeRepository,
                                  ApprovalHistoryRepository approvalRepository,
                                  UserRepository userRepository,
                                  FileStorageService fileStorageService,
                                  EmployeeMediaVersionRepository mediaVersionRepository) {
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.mediaVersionRepository = mediaVersionRepository;
    }

    @Transactional
    public void createRequest(EmployeeRequestForm form, String username) {
        if (form.getSignatureValidUntil().isBefore(form.getSignatureValidFrom())) {
            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date."
            );
        }
        String code = form.getEmployeeCode().trim();
        if (employeeRepository.existsByEmployeeNumber(code)) throw new IllegalArgumentException("Employee code already exists");
        if (requestRepository.existsByEmployeeCodeAndStatusIn(code,
                List.of(RequestStatus.PENDING_DGM, RequestStatus.PENDING_GM))) {
            throw new IllegalArgumentException("A pending request already exists for this employee code");
        }
        User requester = userRepository.findByUsername(username).orElseThrow();
        fileStorageService.validateImage(form.getPhoto());
        fileStorageService.validateImage(form.getSignature());
        if (form.getForeignSignature() != null
                && !form.getForeignSignature().isEmpty()) {

            fileStorageService.validateImage(form.getForeignSignature());
        }
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployeeCode(code);
        request.setEmployeeName(form.getEmployeeName().trim());
        request.setDesignation(form.getDesignation().trim());
        request.setDepartment(form.getDepartment().trim());
        request.setBranch(form.getBranch().trim());
        request.setRemark(form.getRemark().trim());
        request.setPhotoPath(fileStorageService.storeImage(form.getPhoto(), "employee-photo"));
        request.setSignaturePath(fileStorageService.storeImage(form.getSignature(), "employee-signature"));
        if (form.getForeignSignature() != null
                && !form.getForeignSignature().isEmpty()) {

            request.setForeignSignaturePath(
                    fileStorageService.storeImage(
                            form.getForeignSignature(),
                            "pending-foreign-signature"
                    )
            );
        }

        request.setSignatureValidFrom(form.getSignatureValidFrom());
        request.setSignatureValidUntil(form.getSignatureValidUntil());

        request.setRequestedBy(requester);
        requestRepository.save(request);
    }

    @Transactional
    public void createUpdateRequest(Long employeeId, EmployeeUpdateForm form, String username) {
        if (form.getSignatureValidFrom() == null || form.getSignatureValidUntil() == null) {
            throw new IllegalArgumentException("Signature validity dates are required");
        }

        if (form.getSignatureValidUntil().isBefore(form.getSignatureValidFrom())) {
            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date."
            );
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        List<RequestStatus> pendingStatuses = List.of(RequestStatus.PENDING_DGM, RequestStatus.PENDING_GM);
        if (requestRepository.existsByTargetEmployeeIdAndStatusIn(employeeId, pendingStatuses)) {
            throw new IllegalArgumentException("A pending update request already exists for this employee");
        }
        String code = form.getEmployeeCode().trim();
        if (employeeRepository.existsByEmployeeNumberAndIdNot(code, employeeId)) {
            throw new IllegalArgumentException("Employee code already exists");
        }
        if (requestRepository.existsByEmployeeCodeAndStatusIn(code, pendingStatuses)) {
            throw new IllegalArgumentException("A pending request already exists for this employee code");
        }

        User requester = userRepository.findByUsername(username).orElseThrow();
        String photoPath = employee.getPhotoPath();
        String signaturePath = employee.getSignaturePath();
        String foreignSignaturePath = employee.getForeignSignaturePath();

        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            fileStorageService.validateImage(form.getPhoto());
            photoPath = fileStorageService.storeImage(form.getPhoto(), "pending-photo");
        }
        if (form.getSignature() != null && !form.getSignature().isEmpty()) {
            fileStorageService.validateImage(form.getSignature());
            signaturePath = fileStorageService.storeImage(form.getSignature(), "pending-signature");
        }
        if (form.getForeignSignature() != null
                && !form.getForeignSignature().isEmpty()) {

            fileStorageService.validateImage(form.getForeignSignature());

            foreignSignaturePath =
                    fileStorageService.storeImage(
                            form.getForeignSignature(),
                            "pending-foreign-signature"
                    );
        }

        EmployeeRequest request = new EmployeeRequest();
        request.setRequestedBy(requester);
        request.setTargetEmployee(employee);
        request.setEmployeeCode(code);
        request.setEmployeeName(form.getEmployeeName().trim());
        request.setDesignation(form.getDesignation().trim());
        request.setDepartment(form.getDepartment().trim());
        request.setBranch(form.getBranch().trim());
        request.setPhotoPath(photoPath);
        request.setSignaturePath(signaturePath);
        request.setRemark(form.getRemark().trim());
        request.setSignatureValidFrom(form.getSignatureValidFrom());
        request.setSignatureValidUntil(form.getSignatureValidUntil());
        request.setForeignSignaturePath(foreignSignaturePath);

        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getRequestsForUser(String username, int page) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return requestRepository.findByRequestedByIdOrderByRequestedAtDesc(user.getId(), PageRequest.of(page, 20));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getPendingRequests(RequestStatus status, int page) {
        return requestRepository.findByStatusOrderByRequestedAtAsc(status, PageRequest.of(page, 20));
    }

    @Transactional(readOnly = true)
    public EmployeeRequest getRequest(Long id) {
        return requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    @Transactional
    public void dgmDecision(Long id, String action, String remark, String username) {
        EmployeeRequest request = requireStatus(id, RequestStatus.PENDING_DGM);
        User actor = userRepository.findByUsername(username).orElseThrow();
        ApprovalAction approvalAction = parseAction(action);

        saveHistory(request, actor, "DGM", approvalAction, remark);

        request.setStatus(approvalAction == ApprovalAction.APPROVED
                ? RequestStatus.PENDING_GM
                : RequestStatus.REJECTED);

        if (request.getStatus() == RequestStatus.REJECTED) {
            request.setUpdatedAfterRejection(false);
            request.setUpdateRequestStatus(true);
            request.setCompletedAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void gmDecision(Long id, String action, String remark, String username) {
        EmployeeRequest request = requireStatus(id, RequestStatus.PENDING_GM);
        User actor = userRepository.findByUsername(username).orElseThrow();
        ApprovalAction approvalAction = parseAction(action);

        saveHistory(request, actor, "GM", approvalAction, remark);

        if (approvalAction == ApprovalAction.APPROVED) {
            Employee employee = request.getTargetEmployee();

            if (employee == null) {
                if (employeeRepository.existsByEmployeeNumber(request.getEmployeeCode())) {
                    throw new IllegalArgumentException("Employee code already became active");
                }
                employee = new Employee();
            } else if (employeeRepository.existsByEmployeeNumberAndIdNot(
                    request.getEmployeeCode(), employee.getId())) {
                throw new IllegalArgumentException("Employee code already exists");
            }

            employee.setEmployeeNumber(request.getEmployeeCode());
            employee.setFullName(request.getEmployeeName());
            employee.setDesignation(request.getDesignation());
            employee.setDepartment(request.getDepartment());
            employee.setBranchCode(request.getBranch());
            employee.setPhotoPath(request.getPhotoPath());
            employee.setSignaturePath(request.getSignaturePath());
            employee.setSignatureValidFrom(request.getSignatureValidFrom());
            employee.setSignatureValidUntil(request.getSignatureValidUntil());
            employee.setForeignSignaturePath(
                    request.getForeignSignaturePath()
            );

            employee = employeeRepository.saveAndFlush(employee);

            String approvedPhotoPath = fileStorageService.organizeEmployeeImage(
                    request.getPhotoPath(), "profile", employee.getId());

            String approvedSignaturePath = fileStorageService.organizeEmployeeImage(
                    request.getSignaturePath(), "signature", employee.getId());
            String approvedForeignSignaturePath = null;

            if (request.getForeignSignaturePath() != null
                    && !request.getForeignSignaturePath().isBlank()) {

                approvedForeignSignaturePath =
                        fileStorageService.organizeEmployeeImage(
                                request.getForeignSignaturePath(),
                                "foreign-signature",
                                employee.getId()
                        );
            }
            employee.setPhotoPath(approvedPhotoPath);
            employee.setSignaturePath(approvedSignaturePath);
            employee.setForeignSignaturePath(approvedForeignSignaturePath);

            request.setPhotoPath(approvedPhotoPath);
            request.setSignaturePath(approvedSignaturePath);
            request.setForeignSignaturePath(approvedForeignSignaturePath);


            employeeRepository.save(employee);

            EmployeeMediaVersion mediaVersion = new EmployeeMediaVersion();
            mediaVersion.setEmployee(employee);
            mediaVersion.setRequest(request);
            mediaVersion.setVersionNumber(
                    (int) mediaVersionRepository.countByEmployeeId(employee.getId()) + 1
            );
            mediaVersion.setPhotoPath(approvedPhotoPath);
            mediaVersion.setSignaturePath(approvedSignaturePath);
            mediaVersion.setForeignSignaturePath(
                    approvedForeignSignaturePath
            );
            mediaVersionRepository.save(mediaVersion);

            request.setStatus(RequestStatus.APPROVED);

        } else {
            // GM rejected - keep images
            request.setStatus(RequestStatus.REJECTED);
            request.setUpdatedAfterRejection(false);
            request.setUpdateRequestStatus(true);
        }

        request.setCompletedAt(LocalDateTime.now());
    }

    private EmployeeRequest requireStatus(Long id, RequestStatus expected) {
        EmployeeRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (request.getStatus() != expected) throw new IllegalArgumentException("Request is no longer pending at this approval level");
        return request;
    }

    private ApprovalAction parseAction(String action) {
        if ("approve".equals(action)) return ApprovalAction.APPROVED;
        if ("reject".equals(action)) return ApprovalAction.REJECTED;
        throw new IllegalArgumentException("Invalid approval action");
    }

    private void deleteRejectedPendingImages(EmployeeRequest request) {
        fileStorageService.deletePendingImage(request.getPhotoPath());
        fileStorageService.deletePendingImage(request.getSignaturePath());
    }

    private void saveHistory(EmployeeRequest request, User actor, String level,
                             ApprovalAction action, String remark) {
        if (remark == null || remark.isBlank()) throw new IllegalArgumentException("Remark is required");
        ApprovalHistory history = new ApprovalHistory();
        history.setRequest(request);
        history.setActedBy(actor);
        history.setApprovalLevel(level);
        history.setAction(action);
        history.setRemark(remark.trim());
        approvalRepository.save(history);
    }
    public Long getTargetEmployeeIdForUpdate(Long requestId, String username) {
        EmployeeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!request.getRequestedBy().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to update this request");
        }

        if (!request.isUpdateRequest()) {
            throw new IllegalArgumentException("This is not an employee update request");
        }
        if (!request.isUpdateRequestStatus()) {
            throw new IllegalStateException("This request is not available for update");
        }

        return request.getTargetEmployee().getId();
    }
    @Transactional
    public void markUpdateRequestCompleted(Long requestId) {
        EmployeeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        request.setUpdateRequestStatus(false);
    }

    @Transactional
    public void updateRequest(
            Long requestId,
            EmployeeRequest updatedRequest,
            String username) {

        EmployeeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Request not found"));

        // Security: only the original requester can update it
        if (!request.getRequestedBy()
                .getUsername()
                .equals(username)) {

            throw new IllegalArgumentException(
                    "You are not authorized to update this request"
            );
        }

        // Only rejected requests can be updated
        if (request.getStatus() != RequestStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only rejected requests can be updated"
            );
        }

        // Validate dates
        if (updatedRequest.getSignatureValidFrom() == null
                || updatedRequest.getSignatureValidUntil() == null) {

            throw new IllegalArgumentException(
                    "Signature validity dates are required"
            );
        }

        if (updatedRequest.getSignatureValidUntil()
                .isBefore(updatedRequest.getSignatureValidFrom())) {

            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date."
            );
        }

        String code = updatedRequest.getEmployeeCode().trim();

        /*
         * If this request belongs to an existing employee,
         * make sure the new employee code is not being used
         * by another employee.
         */
        if (request.getTargetEmployee() != null) {

            Long employeeId = request.getTargetEmployee().getId();

            if (employeeRepository.existsByEmployeeNumberAndIdNot(
                    code,
                    employeeId)) {

                throw new IllegalArgumentException(
                        "Employee code already exists"
                );
            }

        } else {

            // New employee request
            if (employeeRepository.existsByEmployeeNumber(code)) {
                throw new IllegalArgumentException(
                        "Employee code already exists"
                );
            }
        }

        /*
         * Check for another pending request using this code.
         * Exclude the current request.
         */
        List<RequestStatus> pendingStatuses =
                List.of(
                        RequestStatus.PENDING_DGM,
                        RequestStatus.PENDING_GM
                );

        boolean duplicatePendingRequest =
                requestRepository.existsByEmployeeCodeAndStatusIn(
                        code,
                        pendingStatuses
                );

        if (duplicatePendingRequest) {
            throw new IllegalArgumentException(
                    "A pending request already exists for this employee code"
            );
        }

        /*
         * Update the existing EmployeeRequest.
         *
         * DO NOT replace:
         * requestedBy
         * targetEmployee
         * requestedAt
         * id
         * photoPath
         * signaturePath
         *
         * unless you explicitly want those fields changed.
         */
        request.setEmployeeCode(code);
        request.setEmployeeName(
                updatedRequest.getEmployeeName().trim()
        );
        request.setDesignation(
                updatedRequest.getDesignation().trim()
        );
        request.setDepartment(
                updatedRequest.getDepartment().trim()
        );
        request.setBranch(
                updatedRequest.getBranch().trim()
        );

        request.setSignatureValidFrom(
                updatedRequest.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                updatedRequest.getSignatureValidUntil()
        );

        request.setRemark(
                updatedRequest.getRemark().trim()
        );

        /*
         * This is no longer a rejected request waiting
         * for PD to update it.
         */
        request.setUpdatedAfterRejection(true);
        request.setUpdateRequestStatus(false);

        /*
         * Send the SAME request back to DGM.
         */
        request.setStatus(RequestStatus.PENDING_DGM);

        request.setCompletedAt(null);

        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<Employee> searchEmployeesWithoutPendingRequest(String query, int page) {

        String text = query == null ? "" : query.trim();

        List<RequestStatus> pendingStatuses = List.of(
                RequestStatus.PENDING_DGM,
                RequestStatus.PENDING_GM
        );

        List<EmployeeRequest> pendingRequests =
                requestRepository.findByStatusIn(pendingStatuses);

        List<Long> pendingEmployeeIds = pendingRequests.stream()
                .map(EmployeeRequest::getTargetEmployee)
                .filter(employee -> employee != null)
                .map(Employee::getId)
                .toList();

        Page<Employee> employees =
                employeeRepository
                        .findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                                text,
                                text,
                                PageRequest.of(page, 20)
                        );

        List<Employee> filtered = employees.getContent()
                .stream()
                .filter(employee -> !pendingEmployeeIds.contains(employee.getId()))
                .toList();

        return new PageImpl<>(
                filtered,
                employees.getPageable(),
                filtered.size()
        );
    }


}
