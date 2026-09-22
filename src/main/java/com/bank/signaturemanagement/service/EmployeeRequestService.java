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
     * Create a new employee request.
     *
     * The proposed serial numbers are stored in employee_requests.
     * They are copied to employee_serial_number_history only after
     * final GM approval.
     */
    @Transactional
    public void createRequest(
            EmployeeRequestForm form,
            String username
    ) {
        requirePdOrAdmin(username);

        validateOptionalSignatureDates(
                form.getSignature(),
                form.getForeignSignature(),
                form.getSignatureValidFrom(),
                form.getSignatureValidUntil()
        );

        String code =
                employeeNumberPolicy.normalize(
                        form.getEmployeeCode()
                );

        validateEmployeeCodeAvailable(
                code,
                null
        );

        int newLocalSerial =
                normalizeSerialNumber(
                        form.getLocalSerialNumber()
                );

        int newForeignSerial =
                normalizeSerialNumber(
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

        EmployeeRequest request =
                new EmployeeRequest();

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
         * A newly created employee has no previous serial numbers.
         */
        request.setNewLocalSerial(newLocalSerial);
        request.setOldLocalSerial(0);

        request.setNewForeignSerial(newForeignSerial);
        request.setOldForeignSerial(0);

        requestRepository.save(request);

        auditService.record(
                username,
                "EMPLOYEE_CREATE_PROPOSE",
                "EMPLOYEE_REQUEST",
                String.valueOf(request.getId()),
                null,
                "SUCCESS",
                null,
                code,
                form.getRemark()
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
     * Creates an employee-details update request.
     *
     * EmployeeUpdateForm currently does not contain editable serial fields.
     * Therefore, the current approved serials are copied into the request
     * unchanged.
     */
    @Transactional
    public void createUpdateRequest(
            Long employeeId,
            EmployeeUpdateForm form,
            String username,
            EmployeeChangeProposal proposal
    ) {
        requirePdOrAdmin(username);

        validateDates(
                form.getSignatureValidFrom(),
                form.getSignatureValidUntil()
        );

        Employee employee =
                employeeRepository.findById(employeeId)
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

        String code =
                employeeNumberPolicy.normalize(
                        form.getEmployeeCode()
                );

        if (!code.equals(employee.getEmployeeNumber())) {
            throw new IllegalArgumentException(
                    "Employee ID is immutable"
            );
        }

        validateEmployeeCodeAvailable(
                code,
                employeeId
        );

        EmployeeSerialNumber latestSerial =
                serialNumberRepository
                        .findTopByEmployee_IdOrderByCreatedAtDesc(
                                employeeId
                        )
                        .orElse(null);

        int currentLocalSerial =
                latestSerial != null
                        ? normalizeSerialNumber(
                        latestSerial.getNewLocalSerial()
                )
                        : 0;

        int currentForeignSerial =
                latestSerial != null
                        ? normalizeSerialNumber(
                        latestSerial.getNewForeignSerial()
                )
                        : 0;

        EmployeeRequest request =
                new EmployeeRequest();

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
         * Serial numbers are preserved because this form currently
         * updates employee details only.
         */
        request.setOldLocalSerial(currentLocalSerial);
        request.setNewLocalSerial(currentLocalSerial);

        request.setOldForeignSerial(currentForeignSerial);
        request.setNewForeignSerial(currentForeignSerial);

        requestRepository.save(request);

        auditService.record(
                username,
                "EMPLOYEE_UPDATE_PROPOSE",
                "EMPLOYEE",
                String.valueOf(employeeId),
                null,
                "SUCCESS",
                null,
                null,
                form.getRemark()
        );
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getRequestsForUser(
            String username,
            int page
    ) {
        User user =
                requireUser(username);

        return requestRepository
                .findByRequestedByIdOrderByRequestedAtDesc(
                        user.getId(),
                        PageRequest.of(page, 20)
                );
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getPendingRequests(
            RequestStatus status,
            int page
    ) {
        return requestRepository
                .findByStatusOrderByRequestedAtAsc(
                        status,
                        PageRequest.of(page, 20)
                );
    }

    @Transactional(readOnly = true)
    public EmployeeRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Request not found"
                        )
                );
    }

    @Transactional
    public void dgmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        accessControl.require(
                username,
                "APPROVE_DGM"
        );

        requireApprovalActor(
                username,
                "DGM"
        );

        EmployeeRequest request =
                requireStatus(
                        id,
                        RequestStatus.PENDING_DGM
                );

        ApprovalAction decision =
                parseAction(action);

        saveHistory(
                request,
                requireUser(username),
                "DGM",
                decision,
                remark
        );

        request.setStatus(
                decision == ApprovalAction.APPROVED
                        ? RequestStatus.PENDING_GM
                        : RequestStatus.REJECTED
        );

        if (decision == ApprovalAction.APPROVED) {
            changeProposalService.markPendingGm(
                    request.getChangeProposal()
            );
        }

        if (decision == ApprovalAction.REJECTED) {
            markRejected(request);
        }

        auditService.record(
                username,
                "EMPLOYEE_DGM_" + decision.name(),
                "EMPLOYEE_REQUEST",
                String.valueOf(id),
                null,
                "SUCCESS",
                null,
                request.getStatus().name(),
                remark
        );
    }

    @Transactional
    public void gmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {
        accessControl.require(
                username,
                "APPROVE_GM"
        );

        requireApprovalActor(
                username,
                "GM"
        );

        EmployeeRequest request =
                requireStatus(
                        id,
                        RequestStatus.PENDING_GM
                );

        ApprovalAction decision =
                parseAction(action);

        saveHistory(
                request,
                requireUser(username),
                "GM",
                decision,
                remark
        );

        if (decision == ApprovalAction.REJECTED) {
            request.setStatus(
                    RequestStatus.REJECTED
            );

            markRejected(request);

            auditService.record(
                    username,
                    "EMPLOYEE_GM_REJECTED",
                    "EMPLOYEE_REQUEST",
                    String.valueOf(id),
                    null,
                    "SUCCESS",
                    null,
                    "REJECTED",
                    remark
            );

            return;
        }

        /*
         * Validate serial availability again during final approval.
         * This prevents a serial from being approved twice if another
         * request received approval while this request was pending.
         */
        validateRequestedSerialNumbers(
                normalizeSerialNumber(
                        request.getNewLocalSerial()
                ),
                normalizeSerialNumber(
                        request.getNewForeignSerial()
                ),
                request.getTargetEmployee() != null
                        ? request.getTargetEmployee().getId()
                        : null
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

        employee =
                employeeRepository.saveAndFlush(
                        employee
                );

        /*
         * Employee details remain in Employee.
         * Approved serial values are saved separately in
         * EmployeeSerialNumber.
         */
        saveApprovedSerialNumbers(
                employee,
                request
        );

        organizeApprovedImages(
                employee,
                request
        );

        employeeRepository.save(employee);

        saveMediaVersion(
                employee,
                request
        );

        EmployeeVersion version =
                employeeVersionService.append(
                        employee,
                        username,
                        request.getChangeProposal() == null
                                ? "Approved employee request"
                                : "Approved locked-record change: "
                                  + request.getChangeProposal()
                                .getJustification()
                );

        changeProposalService.markEffective(
                request.getChangeProposal()
        );

        request.setStatus(
                RequestStatus.APPROVED
        );

        request.setUpdateRequestStatus(false);

        request.setCompletedAt(
                LocalDateTime.now()
        );

        auditService.record(
                username,
                "EMPLOYEE_GM_APPROVED",
                "EMPLOYEE",
                String.valueOf(employee.getId()),
                null,
                "SUCCESS",
                oldSnapshot,
                version.getSnapshotJson(),
                remark
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

        return request.getTargetEmployee().getId();
    }

    @Transactional
    public void markUpdateRequestCompleted(
            Long requestId
    ) {
        getRequest(requestId)
                .setUpdateRequestStatus(false);
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
     * Resubmits a rejected request.
     *
     * The existing persistent request is updated selectively.
     * Its proposed serial numbers are therefore preserved.
     */
    @Transactional
    public void updateRequest(
            Long requestId,
            EmployeeRequest updatedRequest,
            MultipartFile foreignSignature,
            String username
    ) {
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

        validateDates(
                updatedRequest.getSignatureValidFrom(),
                updatedRequest.getSignatureValidUntil()
        );

        String code =
                employeeNumberPolicy.normalize(
                        updatedRequest.getEmployeeCode()
                );

        Long targetId =
                request.getTargetEmployee() == null
                        ? null
                        : request.getTargetEmployee().getId();

        validateEmployeeCodeAvailable(
                code,
                targetId
        );

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
         * Do not overwrite request serials from updatedRequest.
         * The rejected-request page does not edit these properties.
         */
        request.setNewLocalSerial(
                normalizeSerialNumber(
                        request.getNewLocalSerial()
                )
        );

        request.setOldLocalSerial(
                normalizeSerialNumber(
                        request.getOldLocalSerial()
                )
        );

        request.setNewForeignSerial(
                normalizeSerialNumber(
                        request.getNewForeignSerial()
                )
        );

        request.setOldForeignSerial(
                normalizeSerialNumber(
                        request.getOldForeignSerial()
                )
        );

        request.setUpdatedAfterRejection(true);
        request.setUpdateRequestStatus(false);

        request.setStatus(
                RequestStatus.PENDING_DGM
        );

        request.setCompletedAt(null);

        requestRepository.save(request);

        changeProposalService.markResubmitted(
                request.getChangeProposal()
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
                        .filter(employee ->
                                employee != null
                        )
                        .map(Employee::getId)
                        .toList();

        Page<Employee> employees =
                employeeRepository
                        .findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                                text,
                                text,
                                PageRequest.of(page, 20)
                        );

        List<Employee> filtered =
                employees.getContent()
                        .stream()
                        .filter(employee ->
                                !pendingEmployeeIds.contains(
                                        employee.getId()
                                )
                        )
                        .toList();

        return new PageImpl<>(
                filtered,
                employees.getPageable(),
                filtered.size()
        );
    }

    /*
     * Saves approved request serials to the final serial-history table.
     *
     * For a new employee, a first history row is always created,
     * including when both serials are 0.
     *
     * For an existing employee, a new history row is created only
     * when at least one serial changes.
     */
    private void saveApprovedSerialNumbers(
            Employee employee,
            EmployeeRequest request
    ) {
        EmployeeSerialNumber latestRecord =
                serialNumberRepository
                        .findTopByEmployee_IdOrderByCreatedAtDesc(
                                employee.getId()
                        )
                        .orElse(null);

        int currentLocalSerial =
                latestRecord != null
                        ? normalizeSerialNumber(
                        latestRecord.getNewLocalSerial()
                )
                        : 0;

        int currentForeignSerial =
                latestRecord != null
                        ? normalizeSerialNumber(
                        latestRecord.getNewForeignSerial()
                )
                        : 0;

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
                localChanged
                        ? currentLocalSerial
                        : 0
        );

        history.setNewLocalSerial(
                requestedLocalSerial
        );

        history.setOldForeignSerial(
                foreignChanged
                        ? currentForeignSerial
                        : 0
        );

        history.setNewForeignSerial(
                requestedForeignSerial
        );

        serialNumberRepository.save(history);
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

        if (requestRepository
                .existsByEmployeeCodeAndStatusIn(
                        code,
                        PENDING_STATUSES
                )) {

            throw new IllegalArgumentException(
                    "A pending request already exists for this employee code"
            );
        }
    }

    /*
     * Validates proposed serial values against the approved history table.
     *
     * Serial 0 means not assigned and is not subject to uniqueness checks.
     */
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
        if (employeeId == null) {
            return false;
        }

        return serialNumberRepository
                .findTopByEmployee_IdOrderByCreatedAtDesc(
                        employeeId
                )
                .map(record ->
                        normalizeSerialNumber(
                                record.getNewLocalSerial()
                        ) == serialNumber
                )
                .orElse(false);
    }

    private boolean isCurrentForeignSerial(
            Long employeeId,
            int serialNumber
    ) {
        if (employeeId == null) {
            return false;
        }

        return serialNumberRepository
                .findTopByEmployee_IdOrderByCreatedAtDesc(
                        employeeId
                )
                .map(record ->
                        normalizeSerialNumber(
                                record.getNewForeignSerial()
                        ) == serialNumber
                )
                .orElse(false);
    }

    /*
     * Converts null to 0 and rejects negative numbers.
     */
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
                            + "the valid-from date."
            );
        }
    }

    private User requireUser(String username) {
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
            throw new IllegalArgumentException(
                    message
            );
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
        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException(
                    "Remark is required"
            );
        }

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
    }

    private void requireOriginalRequester(
            EmployeeRequest request,
            String username
    ) {
        if (!request.getRequestedBy()
                .getUsername()
                .equals(username)) {

            throw new IllegalArgumentException(
                    "You are not authorized to update this request"
            );
        }
    }

    private void validateOptionalSignatureDates(
            MultipartFile local,
            MultipartFile foreign,
            LocalDate from,
            LocalDate until
    ) {
        boolean hasSignature =
                (local != null && !local.isEmpty())
                        || (foreign != null && !foreign.isEmpty());

        if (hasSignature || from != null || until != null) {
            validateDates(from, until);
        }
    }

    private void validateOptionalImage(
            MultipartFile file
    ) {
        if (file != null && !file.isEmpty()) {
            fileStorageService.validateImage(file);
        }
    }

    private void requirePdOrAdmin(
            String username
    ) {
        if (!accessControl.hasAnyRole(
                username,
                "PD",
                "ADMIN"
        )) {
            throw new AccessDeniedException(
                    "Only PD or System Admin may submit employee requests"
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
                            + role
                            + " role"
            );
        }
    }

    /*
     * Applies employee details only.
     *
     * Signature serial values are not stored in Employee.
     */
    private void applyApprovedRequest(
            Employee employee,
            EmployeeRequest request
    ) {
        employee.setActive(true);
        employee.setEmployeeNumber(request.getEmployeeCode());
        employee.setFullName(request.getEmployeeName());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(request.getDepartment());
        employee.setBranch(request.getBranch());
        employee.setEmployeeStatus(request.getEmployeeStatus());
        employee.setClassification(request.getClassification());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setPhotoPath(request.getPhotoPath());
        employee.setSignaturePath(request.getSignaturePath());

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

        String foreignSignaturePath = null;

        if (request.getForeignSignaturePath() != null
                && !request.getForeignSignaturePath().isBlank()) {

            foreignSignaturePath =
                    fileStorageService.organizeEmployeeImage(
                            request.getForeignSignaturePath(),
                            "foreign-signature",
                            employee.getId()
                    );
        }

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
        return path == null || path.isBlank()
                ? null
                : fileStorageService.organizeEmployeeImage(
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

        version.setVersionNumber(
                (int) mediaVersionRepository
                        .countByEmployeeId(
                                employee.getId()
                        ) + 1
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
}