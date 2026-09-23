package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.*;
import com.bank.signaturemanagement.repository.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeRequestService {

    /*
     * Existing database status names are retained:
     *
     * PENDING_DGM = Pending Level 1 Checker
     * PENDING_GM  = Pending Level 2 Checker
     */
    public static final List<RequestStatus> PENDING_STATUSES =
            List.of(
                    RequestStatus.PENDING_DGM,
                    RequestStatus.PENDING_GM
            );

    private final EmployeeRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalHistoryRepository approvalRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;
    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final EmployeeStatusRepository employeeStatusRepository;
    private final EmployeeSerialNumberRepository serialNumberRepository;
    private final EmployeeVersionService employeeVersionService;
    private final EmployeeChangeProposalService changeProposalService;
    private final AuditService auditService;
    private final AccessControlService accessControl;
    private final EmployeeNumberPolicyService employeeNumberPolicy;

    public EmployeeRequestService(
            EmployeeRequestRepository requestRepository,
            EmployeeRepository employeeRepository,
            ApprovalHistoryRepository approvalRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            EmployeeMediaVersionRepository mediaVersionRepository,
            DesignationRepository designationRepository,
            DepartmentRepository departmentRepository,
            BranchRepository branchRepository,
            EmployeeStatusRepository employeeStatusRepository,
            EmployeeSerialNumberRepository serialNumberRepository,
            EmployeeVersionService employeeVersionService,
            EmployeeChangeProposalService changeProposalService,
            AuditService auditService,
            AccessControlService accessControl,
            EmployeeNumberPolicyService employeeNumberPolicy
    ) {
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.mediaVersionRepository = mediaVersionRepository;
        this.designationRepository = designationRepository;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
        this.employeeStatusRepository = employeeStatusRepository;
        this.serialNumberRepository = serialNumberRepository;
        this.employeeVersionService = employeeVersionService;
        this.changeProposalService = changeProposalService;
        this.auditService = auditService;
        this.accessControl = accessControl;
        this.employeeNumberPolicy = employeeNumberPolicy;
    }

    /*
     * Creates a new employee request.
     *
     * The request is submitted by the Maker and initially sent to the
     * Level 1 Checker.
     */
    @Transactional
    public void createRequest(
            EmployeeRequestForm form,
            String username
    ) {
        requireMakerOrAdmin(username);

        if (form == null) {
            throw new IllegalArgumentException(
                    "Employee request information is required"
            );
        }

        validateRequiredText(
                form.getEmployeeName(),
                "Employee name is required"
        );

        validateRequiredText(
                form.getRemark(),
                "Remark is required"
        );

        validateOptionalSignatureDates(
                form.getSignature(),
                form.getForeignSignature(),
                form.getSignatureValidFrom(),
                form.getSignatureValidUntil()
        );

        String code = employeeNumberPolicy.normalize(
                form.getEmployeeCode()
        );

        validateEmployeeCodeAvailable(
                code,
                null
        );

        int newLocalSerial = normalizeSerialNumber(
                form.getLocalSerialNumber()
        );

        int newForeignSerial = normalizeSerialNumber(
                form.getForeignSerialNumber()
        );

        validateRequestedSerialNumbers(
                newLocalSerial,
                newForeignSerial,
                null
        );

        validateOptionalImage(form.getPhoto());
        validateOptionalImage(form.getSignature());
        validateOptionalImage(form.getForeignSignature());

        EmployeeRequest request = new EmployeeRequest();

        request.setRequestedBy(
                requireUser(username)
        );

        request.setEmployeeCode(code);

        request.setEmployeeName(
                form.getEmployeeName().trim()
        );

        request.setDesignation(
                requireDesignation(
                        form.getDesignation()
                )
        );

        request.setDepartment(
                requireDepartment(
                        form.getDepartment()
                )
        );

        request.setBranch(
                requireBranch(
                        form.getBranch()
                )
        );

        request.setEmployeeStatus(
                requireEmployeeStatus(
                        form.getStatusId()
                )
        );

        request.setClassification(
                form.getClassification()
        );

        request.setJoiningDate(
                form.getJoiningDate()
        );

        request.setRemark(
                form.getRemark().trim()
        );

        request.setPhotoPath(
                storeOptionalImage(
                        form.getPhoto(),
                        "employee-photo",
                        null
                )
        );

        request.setSignaturePath(
                storeOptionalImage(
                        form.getSignature(),
                        "employee-signature",
                        null
                )
        );

        request.setForeignSignaturePath(
                storeOptionalImage(
                        form.getForeignSignature(),
                        "pending-foreign-signature",
                        null
                )
        );

        request.setSignatureValidFrom(
                form.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                form.getSignatureValidUntil()
        );

        /*
         * A newly created employee has no old serial numbers.
         */
        request.setOldLocalSerial(0);
        request.setNewLocalSerial(newLocalSerial);
        request.setOldForeignSerial(0);
        request.setNewForeignSerial(newForeignSerial);

        /*
         * Maker submission goes to the Level 1 Checker.
         *
         * PENDING_DGM is retained as the technical enum value.
         */
        request.setStatus(
                RequestStatus.PENDING_DGM
        );

        request.setUpdatedAfterRejection(false);
        request.setUpdateRequestStatus(false);
        request.setCompletedAt(null);

        requestRepository.save(request);

        auditService.record(
                username,
                "EMPLOYEE_CREATE_PROPOSE",
                "EMPLOYEE_REQUEST",
                String.valueOf(request.getId()),
                null,
                "SUCCESS",
                null,
                RequestStatus.PENDING_DGM.name(),
                form.getRemark().trim()
        );
    }

    @Transactional
    public void createUpdateRequest(
            Long employeeId,
            EmployeeUpdateForm form,
            String username
    ) {
        createUpdateRequest(
                employeeId,
                form,
                username,
                null
        );
    }

    /*
     * Creates an employee update request.
     *
     * If this request belongs to a locked-record proposal, the proposal
     * is supplied through the proposal argument.
     */
    @Transactional
    public void createUpdateRequest(
            Long employeeId,
            EmployeeUpdateForm form,
            String username,
            EmployeeChangeProposal proposal
    ) {
        requireMakerOrAdmin(username);

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "Employee ID is required"
            );
        }

        if (form == null) {
            throw new IllegalArgumentException(
                    "Employee update information is required"
            );
        }

        validateRequiredText(
                form.getEmployeeName(),
                "Employee name is required"
        );

        validateRequiredText(
                form.getRemark(),
                "Remark is required"
        );

        validateDates(
                form.getSignatureValidFrom(),
                form.getSignatureValidUntil()
        );

        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found"
                        )
                );

        if (requestRepository
                .existsByTargetEmployeeIdAndStatusIn(
                        employeeId,
                        PENDING_STATUSES
                )) {

            throw new IllegalArgumentException(
                    "A pending update request already exists for this employee"
            );
        }

        String code = employeeNumberPolicy.normalize(
                form.getEmployeeCode()
        );

        if (!code.equals(employee.getEmployeeNumber())) {
            throw new IllegalArgumentException(
                    "Employee ID is immutable"
            );
        }

        if (employeeRepository
                .existsByEmployeeNumberAndIdNot(
                        code,
                        employeeId
                )) {

            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        EmployeeSerialNumber latestSerial =
                findLatestSerial(employeeId);

        int currentLocalSerial =
                latestSerial == null
                        ? 0
                        : normalizeSerialNumber(
                        latestSerial.getNewLocalSerial()
                );

        int currentForeignSerial =
                latestSerial == null
                        ? 0
                        : normalizeSerialNumber(
                        latestSerial.getNewForeignSerial()
                );

        validateOptionalImage(form.getPhoto());
        validateOptionalImage(form.getSignature());
        validateOptionalImage(form.getForeignSignature());

        EmployeeRequest request = new EmployeeRequest();

        request.setRequestedBy(
                requireUser(username)
        );

        request.setTargetEmployee(employee);
        request.setChangeProposal(proposal);
        request.setEmployeeCode(code);

        request.setEmployeeName(
                form.getEmployeeName().trim()
        );

        request.setDesignation(
                requireDesignation(
                        form.getDesignationId()
                )
        );

        request.setDepartment(
                requireDepartment(
                        form.getDepartmentId()
                )
        );

        request.setBranch(
                requireBranch(
                        form.getBranchId()
                )
        );

        request.setEmployeeStatus(
                requireEmployeeStatus(
                        form.getStatusId()
                )
        );

        request.setClassification(
                form.getClassification()
        );

        request.setJoiningDate(
                form.getJoiningDate()
        );

        request.setPhotoPath(
                storeOptionalImage(
                        form.getPhoto(),
                        "pending-photo",
                        employee.getPhotoPath()
                )
        );

        request.setSignaturePath(
                storeOptionalImage(
                        form.getSignature(),
                        "pending-signature",
                        employee.getSignaturePath()
                )
        );

        request.setForeignSignaturePath(
                storeOptionalImage(
                        form.getForeignSignature(),
                        "pending-foreign-signature",
                        employee.getForeignSignaturePath()
                )
        );

        request.setRemark(
                form.getRemark().trim()
        );

        request.setSignatureValidFrom(
                form.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                form.getSignatureValidUntil()
        );

        /*
         * EmployeeUpdateForm currently does not edit serial numbers.
         * The current approved serials are therefore preserved.
         */
        request.setOldLocalSerial(currentLocalSerial);
        request.setNewLocalSerial(currentLocalSerial);
        request.setOldForeignSerial(currentForeignSerial);
        request.setNewForeignSerial(currentForeignSerial);

        /*
         * Maker submission goes to Level 1 Checker.
         */
        request.setStatus(
                RequestStatus.PENDING_DGM
        );

        request.setUpdatedAfterRejection(false);
        request.setUpdateRequestStatus(false);
        request.setCompletedAt(null);

        requestRepository.save(request);

        /*
         * A locked-record proposal is moved from MAKER_EDITING to
         * PENDING_LEVEL_1_CHECKER by the main proposal service.
         */
        if (proposal != null) {
            changeProposalService.markSubmitted(
                    proposal.getId(),
                    username
            );
        }

        auditService.record(
                username,
                "EMPLOYEE_UPDATE_PROPOSE",
                "EMPLOYEE_REQUEST",
                String.valueOf(request.getId()),
                null,
                "SUCCESS",
                null,
                RequestStatus.PENDING_DGM.name(),
                form.getRemark().trim()
        );
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getRequestsForUser(
            String username,
            int page
    ) {
        User user = requireUser(username);

        return requestRepository
                .findByRequestedByIdOrderByRequestedAtDesc(
                        user.getId(),
                        PageRequest.of(
                                normalizePage(page),
                                20
                        )
                );
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getPendingRequests(
            RequestStatus status,
            int page
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Request status is required"
            );
        }

        return requestRepository
                .findByStatusOrderByRequestedAtAsc(
                        status,
                        PageRequest.of(
                                normalizePage(page),
                                20
                        )
                );
    }

    @Transactional(readOnly = true)
    public EmployeeRequest getRequest(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Request ID is required"
            );
        }

        return requestRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Request not found"
                        )
                );
    }

    /*
     * Backward-compatible controller method.
     *
     * DGM is now the Level 1 Checker.
     */
    @Transactional
    public void dgmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        level1CheckerDecision(
                id,
                action,
                remark,
                username
        );
    }

    /*
     * Level 1 Checker decision.
     */
    @Transactional
    public void level1CheckerDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        /*
         * APPROVE_DGM is retained as the existing technical permission.
         */
        accessControl.require(
                username,
                "APPROVE_DGM"
        );

        requireApprovalActor(
                username,
                "LEVEL_1_CHECKER"
        );

        EmployeeRequest request = requireStatus(
                id,
                RequestStatus.PENDING_DGM
        );

        ApprovalAction decision =
                parseAction(action);

        saveHistory(
                request,
                requireUser(username),
                "LEVEL_1_CHECKER",
                decision,
                remark
        );

        if (decision == ApprovalAction.APPROVED) {
            /*
             * PENDING_GM is the existing technical value for
             * Pending Level 2 Checker.
             */
            request.setStatus(
                    RequestStatus.PENDING_GM
            );

            request.setCompletedAt(null);
            request.setUpdateRequestStatus(false);

            /*
             * Use the existing main proposal-service method.
             */
            changeProposalService.markPendingLevel2Checker(
                    request.getChangeProposal()
            );
        } else {
            request.setStatus(
                    RequestStatus.REJECTED
            );

            markRejected(request);
        }

        requestRepository.save(request);

        auditService.record(
                username,
                "EMPLOYEE_LEVEL_1_" + decision.name(),
                "EMPLOYEE_REQUEST",
                String.valueOf(id),
                null,
                "SUCCESS",
                RequestStatus.PENDING_DGM.name(),
                request.getStatus().name(),
                remark.trim()
        );
    }

    /*
     * Backward-compatible controller method.
     *
     * GM is now the Level 2 Checker.
     */
    @Transactional
    public void gmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        level2CheckerDecision(
                id,
                action,
                remark,
                username
        );
    }

    /*
     * Level 2 Checker decision.
     */
    @Transactional
    public void level2CheckerDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        /*
         * APPROVE_GM is retained as the existing technical permission.
         */
        accessControl.require(
                username,
                "APPROVE_GM"
        );

        requireApprovalActor(
                username,
                "LEVEL_2_CHECKER"
        );

        EmployeeRequest request = requireStatus(
                id,
                RequestStatus.PENDING_GM
        );

        ApprovalAction decision =
                parseAction(action);

        saveHistory(
                request,
                requireUser(username),
                "LEVEL_2_CHECKER",
                decision,
                remark
        );

        if (decision == ApprovalAction.REJECTED) {
            request.setStatus(
                    RequestStatus.REJECTED
            );

            markRejected(request);

            requestRepository.save(request);

            auditService.record(
                    username,
                    "EMPLOYEE_LEVEL_2_REJECTED",
                    "EMPLOYEE_REQUEST",
                    String.valueOf(id),
                    null,
                    "SUCCESS",
                    RequestStatus.PENDING_GM.name(),
                    RequestStatus.REJECTED.name(),
                    remark.trim()
            );

            return;
        }

        Long targetEmployeeId =
                request.getTargetEmployee() == null
                        ? null
                        : request.getTargetEmployee().getId();

        /*
         * Revalidate serial availability immediately before final approval.
         */
        validateRequestedSerialNumbers(
                normalizeSerialNumber(
                        request.getNewLocalSerial()
                ),
                normalizeSerialNumber(
                        request.getNewForeignSerial()
                ),
                targetEmployeeId
        );

        Employee employee =
                request.getTargetEmployee();

        String oldSnapshot =
                employee == null
                        ? null
                        : employeeVersionService.snapshot(
                        employee
                );

        if (employee != null) {
            employeeVersionService.ensureBaseline(
                    employee,
                    username,
                    "Baseline before approved change"
            );
        }

        if (employee == null) {
            if (employeeRepository.existsByEmployeeNumber(
                    request.getEmployeeCode()
            )) {
                throw new IllegalArgumentException(
                        "Employee code already became active"
                );
            }

            employee = new Employee();
        } else if (
                employeeRepository
                        .existsByEmployeeNumberAndIdNot(
                                request.getEmployeeCode(),
                                employee.getId()
                        )
        ) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        applyApprovedRequest(
                employee,
                request
        );

        employee = employeeRepository.saveAndFlush(
                employee
        );

        /*
         * Approved serial numbers are written to serial-number history.
         */
        saveApprovedSerialNumbers(
                employee,
                request
        );

        organizeApprovedImages(
                employee,
                request
        );

        employee = employeeRepository.saveAndFlush(
                employee
        );

        saveMediaVersion(
                employee,
                request
        );

        String versionReason =
                request.getChangeProposal() == null
                        ? "Approved employee request"
                        : "Approved locked-record change: "
                          + safeJustification(
                        request.getChangeProposal()
                );

        EmployeeVersion version =
                employeeVersionService.append(
                        employee,
                        username,
                        versionReason
                );

        /*
         * Final approval makes the linked proposal effective.
         */
        changeProposalService.markEffective(
                request.getChangeProposal()
        );

        request.setStatus(
                RequestStatus.APPROVED
        );

        request.setUpdatedAfterRejection(false);
        request.setUpdateRequestStatus(false);
        request.setCompletedAt(LocalDateTime.now());

        requestRepository.save(request);

        auditService.record(
                username,
                "EMPLOYEE_LEVEL_2_APPROVED",
                "EMPLOYEE",
                String.valueOf(employee.getId()),
                null,
                "SUCCESS",
                oldSnapshot,
                version.getSnapshotJson(),
                remark.trim()
        );
    }

    @Transactional(readOnly = true)
    public Long getTargetEmployeeIdForUpdate(
            Long requestId,
            String username
    ) {
        EmployeeRequest request =
                getRequest(requestId);

        requireOriginalRequester(
                request,
                username
        );

        if (!request.isUpdateRequest()) {
            throw new IllegalArgumentException(
                    "This is not an employee update request"
            );
        }

        if (!request.isUpdateRequestStatus()) {
            throw new IllegalStateException(
                    "This request is not available for update"
            );
        }

        if (request.getTargetEmployee() == null) {
            throw new IllegalStateException(
                    "Target employee is not available"
            );
        }

        return request
                .getTargetEmployee()
                .getId();
    }

    @Transactional
    public void markUpdateRequestCompleted(
            Long requestId
    ) {
        EmployeeRequest request =
                getRequest(requestId);

        request.setUpdateRequestStatus(false);

        requestRepository.save(request);
    }

    @Transactional
    public void updateRequest(
            Long requestId,
            EmployeeRequest updatedRequest,
            String username
    ) {
        updateRequest(
                requestId,
                updatedRequest,
                null,
                username
        );
    }

    /*
     * Corrects and resubmits a rejected request.
     *
     * The corrected request returns to the Level 1 Checker.
     */
    @Transactional
    public void updateRequest(
            Long requestId,
            EmployeeRequest updatedRequest,
            MultipartFile foreignSignature,
            String username
    ) {
        requireMakerOrAdmin(username);

        if (updatedRequest == null) {
            throw new IllegalArgumentException(
                    "Updated request information is required"
            );
        }

        EmployeeRequest request =
                getRequest(requestId);

        requireOriginalRequester(
                request,
                username
        );

        if (request.getStatus() != RequestStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only rejected requests can be updated"
            );
        }

        validateRequiredText(
                updatedRequest.getEmployeeName(),
                "Employee name is required"
        );

        validateRequiredText(
                updatedRequest.getRemark(),
                "Remark is required"
        );

        validateDates(
                updatedRequest.getSignatureValidFrom(),
                updatedRequest.getSignatureValidUntil()
        );

        validateOptionalImage(
                foreignSignature
        );

        String code = employeeNumberPolicy.normalize(
                updatedRequest.getEmployeeCode()
        );

        Long targetId =
                request.getTargetEmployee() == null
                        ? null
                        : request.getTargetEmployee().getId();

        if (targetId == null) {
            if (employeeRepository.existsByEmployeeNumber(code)) {
                throw new IllegalArgumentException(
                        "Employee code already exists"
                );
            }
        } else if (
                employeeRepository
                        .existsByEmployeeNumberAndIdNot(
                                code,
                                targetId
                        )
        ) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        request.setEmployeeCode(code);

        request.setEmployeeName(
                updatedRequest.getEmployeeName().trim()
        );

        request.setDesignation(
                requireEntity(
                        updatedRequest.getDesignation(),
                        "Designation is required"
                )
        );

        request.setDepartment(
                requireEntity(
                        updatedRequest.getDepartment(),
                        "Department is required"
                )
        );

        request.setBranch(
                requireEntity(
                        updatedRequest.getBranch(),
                        "Branch is required"
                )
        );

        if (updatedRequest.getEmployeeStatus() != null) {
            request.setEmployeeStatus(
                    updatedRequest.getEmployeeStatus()
            );
        }

        request.setClassification(
                updatedRequest.getClassification()
        );

        request.setJoiningDate(
                updatedRequest.getJoiningDate()
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

        request.setForeignSignaturePath(
                storeOptionalImage(
                        foreignSignature,
                        "pending-foreign-signature",
                        request.getForeignSignaturePath()
                )
        );

        /*
         * Keep all serial values non-null.
         */
        request.setOldLocalSerial(
                normalizeSerialNumber(
                        request.getOldLocalSerial()
                )
        );

        request.setNewLocalSerial(
                normalizeSerialNumber(
                        request.getNewLocalSerial()
                )
        );

        request.setOldForeignSerial(
                normalizeSerialNumber(
                        request.getOldForeignSerial()
                )
        );

        request.setNewForeignSerial(
                normalizeSerialNumber(
                        request.getNewForeignSerial()
                )
        );

        validateRequestedSerialNumbers(
                request.getNewLocalSerial(),
                request.getNewForeignSerial(),
                targetId
        );

        request.setUpdatedAfterRejection(true);
        request.setUpdateRequestStatus(false);

        /*
         * Resubmission returns to Level 1 Checker.
         */
        request.setStatus(
                RequestStatus.PENDING_DGM
        );

        request.setCompletedAt(null);

        requestRepository.save(request);

        /*
         * The main proposal service uses PENDING_LEVEL_1_CHECKER here.
         */
        changeProposalService.markResubmitted(
                request.getChangeProposal()
        );

        auditService.record(
                username,
                "EMPLOYEE_REQUEST_RESUBMITTED",
                "EMPLOYEE_REQUEST",
                String.valueOf(request.getId()),
                null,
                "SUCCESS",
                RequestStatus.REJECTED.name(),
                RequestStatus.PENDING_DGM.name(),
                request.getRemark()
        );
    }

    @Transactional(readOnly = true)
    public Page<Employee> searchEmployeesWithoutPendingRequest(
            String query,
            int page
    ) {
        String text =
                query == null
                        ? ""
                        : query.trim();

        List<Long> pendingEmployeeIds =
                requestRepository
                        .findByStatusIn(PENDING_STATUSES)
                        .stream()
                        .map(EmployeeRequest::getTargetEmployee)
                        .filter(employee -> employee != null)
                        .map(Employee::getId)
                        .distinct()
                        .toList();

        Page<Employee> employeePage =
                employeeRepository
                        .findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                                text,
                                text,
                                PageRequest.of(
                                        normalizePage(page),
                                        20
                                )
                        );

        List<Employee> filtered =
                employeePage
                        .getContent()
                        .stream()
                        .filter(employee ->
                                !pendingEmployeeIds.contains(
                                        employee.getId()
                                )
                        )
                        .toList();

        return new PageImpl<>(
                filtered,
                employeePage.getPageable(),
                filtered.size()
        );
    }

    /*
     * Saves final approved serial numbers.
     */
    private void saveApprovedSerialNumbers(
            Employee employee,
            EmployeeRequest request
    ) {
        EmployeeSerialNumber latestRecord =
                findLatestSerial(employee.getId());

        int currentLocalSerial =
                latestRecord == null
                        ? 0
                        : normalizeSerialNumber(
                        latestRecord.getNewLocalSerial()
                );

        int currentForeignSerial =
                latestRecord == null
                        ? 0
                        : normalizeSerialNumber(
                        latestRecord.getNewForeignSerial()
                );

        int requestedLocalSerial =
                normalizeSerialNumber(
                        request.getNewLocalSerial()
                );

        int requestedForeignSerial =
                normalizeSerialNumber(
                        request.getNewForeignSerial()
                );

        boolean localChanged =
                latestRecord == null
                        || requestedLocalSerial
                        != currentLocalSerial;

        boolean foreignChanged =
                latestRecord == null
                        || requestedForeignSerial
                        != currentForeignSerial;

        if (!localChanged && !foreignChanged) {
            return;
        }

        validateApprovedSerialAvailability(
                requestedLocalSerial,
                requestedForeignSerial,
                currentLocalSerial,
                currentForeignSerial
        );

        EmployeeSerialNumber history =
                new EmployeeSerialNumber();

        history.setEmployee(employee);

        history.setOldLocalSerial(
                currentLocalSerial
        );

        history.setNewLocalSerial(
                requestedLocalSerial
        );

        history.setOldForeignSerial(
                currentForeignSerial
        );

        history.setNewForeignSerial(
                requestedForeignSerial
        );

        serialNumberRepository.save(history);
    }

    private EmployeeSerialNumber findLatestSerial(
            Long employeeId
    ) {
        if (employeeId == null) {
            return null;
        }

        return serialNumberRepository
                .findTopByEmployee_IdOrderByCreatedAtDesc(
                        employeeId
                )
                .orElse(null);
    }

    private void validateEmployeeCodeAvailable(
            String code,
            Long targetEmployeeId
    ) {
        boolean employeeExists =
                targetEmployeeId == null
                        ? employeeRepository
                        .existsByEmployeeNumber(code)
                        : employeeRepository
                        .existsByEmployeeNumberAndIdNot(
                                code,
                                targetEmployeeId
                        );

        if (employeeExists) {
            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }

        if (targetEmployeeId == null
                && requestRepository
                .existsByEmployeeCodeAndStatusIn(
                        code,
                        PENDING_STATUSES
                )) {

            throw new IllegalArgumentException(
                    "A pending request already exists for this employee code"
            );
        }
    }

    private void validateRequestedSerialNumbers(
            int localSerial,
            int foreignSerial,
            Long employeeId
    ) {
        if (localSerial > 0
                && serialNumberRepository
                .existsByNewLocalSerial(localSerial)
                && !isCurrentLocalSerial(
                employeeId,
                localSerial
        )) {

            throw new IllegalStateException(
                    "Local serial number "
                            + localSerial
                            + " is already assigned"
            );
        }

        if (foreignSerial > 0
                && serialNumberRepository
                .existsByNewForeignSerial(foreignSerial)
                && !isCurrentForeignSerial(
                employeeId,
                foreignSerial
        )) {

            throw new IllegalStateException(
                    "Foreign serial number "
                            + foreignSerial
                            + " is already assigned"
            );
        }
    }

    private void validateApprovedSerialAvailability(
            int newLocalSerial,
            int newForeignSerial,
            int currentLocalSerial,
            int currentForeignSerial
    ) {
        if (newLocalSerial > 0
                && newLocalSerial != currentLocalSerial
                && serialNumberRepository
                .existsByNewLocalSerial(
                        newLocalSerial
                )) {

            throw new IllegalStateException(
                    "Local serial number "
                            + newLocalSerial
                            + " is already assigned"
            );
        }

        if (newForeignSerial > 0
                && newForeignSerial != currentForeignSerial
                && serialNumberRepository
                .existsByNewForeignSerial(
                        newForeignSerial
                )) {

            throw new IllegalStateException(
                    "Foreign serial number "
                            + newForeignSerial
                            + " is already assigned"
            );
        }
    }

    private boolean isCurrentLocalSerial(
            Long employeeId,
            int serialNumber
    ) {
        EmployeeSerialNumber latest =
                findLatestSerial(employeeId);

        return latest != null
                && normalizeSerialNumber(
                latest.getNewLocalSerial()
        ) == serialNumber;
    }

    private boolean isCurrentForeignSerial(
            Long employeeId,
            int serialNumber
    ) {
        EmployeeSerialNumber latest =
                findLatestSerial(employeeId);

        return latest != null
                && normalizeSerialNumber(
                latest.getNewForeignSerial()
        ) == serialNumber;
    }

    private int normalizeSerialNumber(
            Integer serialNumber
    ) {
        if (serialNumber == null) {
            return 0;
        }

        if (serialNumber < 0) {
            throw new IllegalArgumentException(
                    "Signature serial number cannot be negative"
            );
        }

        return serialNumber;
    }

    private void validateDates(
            LocalDate validFrom,
            LocalDate validUntil
    ) {
        if (validFrom == null || validUntil == null) {
            throw new IllegalArgumentException(
                    "Signature validity dates are required"
            );
        }

        if (validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after "
                            + "the valid-from date"
            );
        }
    }

    private void validateRequiredText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private User requireUser(
            String username
    ) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    private Designation requireDesignation(
            String name
    ) {
        validateRequiredText(
                name,
                "Designation is required"
        );

        return designationRepository
                .findByDesignationName(name.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Designation not found"
                        )
                );
    }

    private Department requireDepartment(
            String name
    ) {
        validateRequiredText(
                name,
                "Department is required"
        );

        return departmentRepository
                .findByDepartmentName(name.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"
                        )
                );
    }

    private Branch requireBranch(
            String name
    ) {
        validateRequiredText(
                name,
                "Branch is required"
        );

        return branchRepository
                .findByBranchName(name.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch not found"
                        )
                );
    }

    private Designation requireDesignation(
            Long id
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Designation is required"
            );
        }

        return designationRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Designation not found"
                        )
                );
    }

    private Department requireDepartment(
            Long id
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Department is required"
            );
        }

        return departmentRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"
                        )
                );
    }

    private Branch requireBranch(
            Long id
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Branch is required"
            );
        }

        return branchRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch not found"
                        )
                );
    }

    private <T> T requireEntity(
            T entity,
            String message
    ) {
        if (entity == null) {
            throw new IllegalArgumentException(message);
        }

        return entity;
    }

    private String storeOptionalImage(
            MultipartFile image,
            String folder,
            String existingPath
    ) {
        if (image == null || image.isEmpty()) {
            return existingPath;
        }

        fileStorageService.validateImage(image);

        return fileStorageService.storeImage(
                image,
                folder
        );
    }

    private EmployeeRequest requireStatus(
            Long id,
            RequestStatus expected
    ) {
        EmployeeRequest request =
                getRequest(id);

        if (request.getStatus() != expected) {
            throw new IllegalArgumentException(
                    "Request is no longer pending at this approval level"
            );
        }

        return request;
    }

    private ApprovalAction parseAction(
            String action
    ) {
        if ("approve".equalsIgnoreCase(action)) {
            return ApprovalAction.APPROVED;
        }

        if ("reject".equalsIgnoreCase(action)) {
            return ApprovalAction.REJECTED;
        }

        throw new IllegalArgumentException(
                "Invalid approval action"
        );
    }

    private void saveHistory(
            EmployeeRequest request,
            User actor,
            String level,
            ApprovalAction action,
            String remark
    ) {
        validateRequiredText(
                remark,
                "Remark is required"
        );

        ApprovalHistory history =
                new ApprovalHistory();

        history.setRequest(request);
        history.setActedBy(actor);
        history.setApprovalLevel(level);
        history.setAction(action);
        history.setRemark(remark.trim());

        approvalRepository.save(history);
    }

    private void markRejected(
            EmployeeRequest request
    ) {
        request.setUpdatedAfterRejection(false);
        request.setUpdateRequestStatus(true);
        request.setCompletedAt(LocalDateTime.now());

        /*
         * The main proposal service already provides markRejected(...).
         */
        changeProposalService.markRejected(
                request.getChangeProposal()
        );
    }

    private void requireOriginalRequester(
            EmployeeRequest request,
            String username
    ) {
        if (request.getRequestedBy() == null
                || request.getRequestedBy().getUsername() == null
                || username == null
                || !request.getRequestedBy()
                .getUsername()
                .equalsIgnoreCase(username)) {

            throw new AccessDeniedException(
                    "You are not authorized to update this request"
            );
        }
    }

    private void validateOptionalSignatureDates(
            MultipartFile localSignature,
            MultipartFile foreignSignature,
            LocalDate validFrom,
            LocalDate validUntil
    ) {
        boolean hasSignature =
                (localSignature != null
                        && !localSignature.isEmpty())
                        || (foreignSignature != null
                        && !foreignSignature.isEmpty());

        boolean hasAnyDate =
                validFrom != null
                        || validUntil != null;

        if (hasSignature || hasAnyDate) {
            validateDates(
                    validFrom,
                    validUntil
            );
        }
    }

    private void validateOptionalImage(
            MultipartFile file
    ) {
        if (file != null && !file.isEmpty()) {
            fileStorageService.validateImage(file);
        }
    }

    private void requireMakerOrAdmin(
            String username
    ) {
        if (!accessControl.hasAnyRole(
                username,
                "MAKER",
                "ADMIN"
        )) {
            throw new AccessDeniedException(
                    "Only Maker or System Admin may submit employee requests"
            );
        }
    }

    private void requireApprovalActor(
            String username,
            String role
    ) {
        if (!accessControl.hasAnyRole(
                username,
                role,
                "ADMIN"
        )) {
            throw new AccessDeniedException(
                    "This approval requires the "
                            + roleLabel(role)
                            + " role"
            );
        }
    }

    private String roleLabel(
            String role
    ) {
        return switch (role) {
            case "MAKER" -> "Maker";
            case "LEVEL_1_CHECKER" -> "Level 1 Checker";
            case "LEVEL_2_CHECKER" -> "Level 2 Checker";
            default -> role;
        };
    }

    /*
     * Applies approved employee details.
     *
     * Serial numbers are intentionally excluded because they are stored
     * in EmployeeSerialNumber.
     */
    private void applyApprovedRequest(
            Employee employee,
            EmployeeRequest request
    ) {
        employee.setActive(true);

        employee.setEmployeeNumber(
                request.getEmployeeCode()
        );

        employee.setFullName(
                request.getEmployeeName()
        );

        employee.setDesignation(
                request.getDesignation()
        );

        employee.setDepartment(
                request.getDepartment()
        );

        employee.setBranch(
                request.getBranch()
        );

        employee.setEmployeeStatus(
                request.getEmployeeStatus()
        );

        employee.setClassification(
                request.getClassification()
        );

        employee.setJoiningDate(
                request.getJoiningDate()
        );

        employee.setPhotoPath(
                request.getPhotoPath()
        );

        employee.setSignaturePath(
                request.getSignaturePath()
        );

        employee.setForeignSignaturePath(
                request.getForeignSignaturePath()
        );

        employee.setSignatureValidFrom(
                request.getSignatureValidFrom()
        );

        employee.setSignatureValidUntil(
                request.getSignatureValidUntil()
        );
    }

    private EmployeeStatus requireEmployeeStatus(
            Long id
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Employee status is required"
            );
        }

        EmployeeStatus status =
                employeeStatusRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid employee status"
                                )
                        );

        if (!status.isActive()) {
            throw new IllegalArgumentException(
                    "Employee status is inactive"
            );
        }

        return status;
    }

    private void organizeApprovedImages(
            Employee employee,
            EmployeeRequest request
    ) {
        String photoPath =
                organizeOptional(
                        request.getPhotoPath(),
                        "profile",
                        employee.getId()
                );

        String signaturePath =
                organizeOptional(
                        request.getSignaturePath(),
                        "signature",
                        employee.getId()
                );

        String foreignSignaturePath =
                organizeOptional(
                        request.getForeignSignaturePath(),
                        "foreign-signature",
                        employee.getId()
                );

        employee.setPhotoPath(photoPath);
        employee.setSignaturePath(signaturePath);
        employee.setForeignSignaturePath(
                foreignSignaturePath
        );

        request.setPhotoPath(photoPath);
        request.setSignaturePath(signaturePath);
        request.setForeignSignaturePath(
                foreignSignaturePath
        );
    }

    private String organizeOptional(
            String path,
            String type,
            Long employeeId
    ) {
        if (path == null || path.isBlank()) {
            return null;
        }

        return fileStorageService.organizeEmployeeImage(
                path,
                type,
                employeeId
        );
    }

    private void saveMediaVersion(
            Employee employee,
            EmployeeRequest request
    ) {
        EmployeeMediaVersion version =
                new EmployeeMediaVersion();

        version.setEmployee(employee);
        version.setRequest(request);

        long existingVersionCount =
                mediaVersionRepository
                        .countByEmployeeId(
                                employee.getId()
                        );

        if (existingVersionCount >= Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Maximum employee media version count reached"
            );
        }

        version.setVersionNumber(
                (int) existingVersionCount + 1
        );

        version.setPhotoPath(
                employee.getPhotoPath()
        );

        version.setSignaturePath(
                employee.getSignaturePath()
        );

        version.setForeignSignaturePath(
                employee.getForeignSignaturePath()
        );

        mediaVersionRepository.save(version);
    }

    private String safeJustification(
            EmployeeChangeProposal proposal
    ) {
        if (proposal == null
                || proposal.getJustification() == null
                || proposal.getJustification().isBlank()) {

            return "No justification provided";
        }

        return proposal
                .getJustification()
                .trim();
    }

    private int normalizePage(
            int page
    ) {
        return Math.max(page, 0);
    }
}