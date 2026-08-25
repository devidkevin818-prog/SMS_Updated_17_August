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

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;


    public EmployeeRequestService(
            EmployeeRequestRepository requestRepository,
            EmployeeRepository employeeRepository,
            ApprovalHistoryRepository approvalRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            EmployeeMediaVersionRepository mediaVersionRepository,
            DesignationRepository designationRepository,
            DepartmentRepository departmentRepository,
            BranchRepository branchRepository
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
    }


    // =========================================================
    // CREATE NEW EMPLOYEE REQUEST
    // =========================================================

    @Transactional
    public void createRequest(EmployeeRequestForm form, String username) {

        if (form.getSignatureValidUntil()
                .isBefore(form.getSignatureValidFrom())) {

            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date."
            );
        }
<<<<<<< HEAD


        String code = form.getEmployeeCode().trim();


        if (employeeRepository.existsByEmployeeNumber(code)) {

            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
=======
        String code = EmployeeNumberFormat.normalize(form.getEmployeeCode());
        if (employeeRepository.existsByEmployeeNumber(code)) throw new IllegalArgumentException("Employee code already exists");
        if (requestRepository.existsByEmployeeCodeAndStatusIn(code,
                List.of(RequestStatus.PENDING_DGM, RequestStatus.PENDING_GM))) {
            throw new IllegalArgumentException("A pending request already exists for this employee code");
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
        }


        if (requestRepository.existsByEmployeeCodeAndStatusIn(
                code,
                List.of(
                        RequestStatus.PENDING_DGM,
                        RequestStatus.PENDING_GM
                )
        )) {

            throw new IllegalArgumentException(
                    "A pending request already exists for this employee code"
            );
        }


        User requester =
                userRepository.findByUsername(username)
                        .orElseThrow();


        fileStorageService.validateImage(form.getPhoto());
        fileStorageService.validateImage(form.getSignature());
<<<<<<< HEAD
=======
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
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8


        // -----------------------------------------------------
        // EmployeeRequestForm still carries names, not ids.
        // If you switch that form to ids too, replace these
        // three lookups with findById(...) like createUpdateRequest.
        // -----------------------------------------------------

        Designation designation =
                designationRepository
                        .findByDesignationName(
                                form.getDesignation().trim()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Designation not found"
                                )
                        );

<<<<<<< HEAD

        Department department =
                departmentRepository
                        .findByDepartmentName(
                                form.getDepartment().trim()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Department not found"
                                )
                        );


        Branch branch =
                branchRepository
                        .findByBranchName(
                                form.getBranch().trim()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Branch not found"
                                )
                        );

=======
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
        String code = EmployeeNumberFormat.normalize(form.getEmployeeCode());
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
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8

        EmployeeRequest request = new EmployeeRequest();

        request.setEmployeeCode(code);
<<<<<<< HEAD

        request.setEmployeeName(
                form.getEmployeeName().trim()
        );

        request.setDesignation(designation);
        request.setDepartment(department);
        request.setBranch(branch);

        request.setRemark(
                form.getRemark().trim()
        );

        request.setPhotoPath(
                fileStorageService.storeImage(
                        form.getPhoto(),
                        "employee-photo"
                )
        );

        request.setSignaturePath(
                fileStorageService.storeImage(
                        form.getSignature(),
                        "employee-signature"
                )
        );

        request.setSignatureValidFrom(
                form.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                form.getSignatureValidUntil()
        );

        request.setRequestedBy(requester);
=======
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
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8

        requestRepository.save(request);
    }


    // =========================================================
    // CREATE UPDATE REQUEST
    // =========================================================

    @Transactional
    public void createUpdateRequest(
            Long employeeId,
            EmployeeUpdateForm form,
            String username
    ) {

<<<<<<< HEAD
        if (form.getSignatureValidFrom() == null ||
                form.getSignatureValidUntil() == null) {
=======
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
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8

            throw new IllegalArgumentException(
                    "Signature validity dates are required"
            );
        }


        if (form.getSignatureValidUntil()
                .isBefore(form.getSignatureValidFrom())) {

            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date."
            );
        }

<<<<<<< HEAD
=======
        String code = EmployeeNumberFormat.normalize(updatedRequest.getEmployeeCode());
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found"
                                )
                        );


        List<RequestStatus> pendingStatuses =
                List.of(
                        RequestStatus.PENDING_DGM,
                        RequestStatus.PENDING_GM
                );


        if (requestRepository
                .existsByTargetEmployeeIdAndStatusIn(
                        employeeId,
                        pendingStatuses
                )) {

            throw new IllegalArgumentException(
                    "A pending update request already exists for this employee"
            );
        }


        String code = form.getEmployeeCode().trim();


        if (employeeRepository
                .existsByEmployeeNumberAndIdNot(
                        code,
                        employeeId
                )) {

            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
        }


        if (requestRepository
                .existsByEmployeeCodeAndStatusIn(
                        code,
                        pendingStatuses
                )) {

            throw new IllegalArgumentException(
                    "A pending request already exists for this employee code"
            );
        }


        User requester =
                userRepository.findByUsername(username)
                        .orElseThrow();


        String photoPath = employee.getPhotoPath();
        String signaturePath = employee.getSignaturePath();


        if (form.getPhoto() != null &&
                !form.getPhoto().isEmpty()) {

            fileStorageService.validateImage(
                    form.getPhoto()
            );

            photoPath =
                    fileStorageService.storeImage(
                            form.getPhoto(),
                            "pending-photo"
                    );
        }


        if (form.getSignature() != null &&
                !form.getSignature().isEmpty()) {

            fileStorageService.validateImage(
                    form.getSignature()
            );

            signaturePath =
                    fileStorageService.storeImage(
                            form.getSignature(),
                            "pending-signature"
                    );
        }


        // -----------------------------------------------------
        // Lookup by id (the form now carries ids, not names)
        // -----------------------------------------------------

        Designation designation =
                designationRepository
                        .findById(form.getDesignationId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Designation not found"
                                )
                        );


        Department department =
                departmentRepository
                        .findById(form.getDepartmentId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Department not found"
                                )
                        );


        Branch branch =
                branchRepository
                        .findById(form.getBranchId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Branch not found"
                                )
                        );


        EmployeeRequest request = new EmployeeRequest();

        request.setRequestedBy(requester);
        request.setTargetEmployee(employee);
        request.setEmployeeCode(code);

        request.setEmployeeName(
                form.getEmployeeName().trim()
        );

        request.setDesignation(designation);
        request.setDepartment(department);
        request.setBranch(branch);

        request.setPhotoPath(photoPath);
        request.setSignaturePath(signaturePath);

        request.setRemark(
                form.getRemark().trim()
        );

        request.setSignatureValidFrom(
                form.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                form.getSignatureValidUntil()
        );

        requestRepository.save(request);
    }


    // =========================================================
    // GET REQUESTS FOR USER
    // =========================================================

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getRequestsForUser(
            String username,
            int page
    ) {

        User user =
                userRepository.findByUsername(username)
                        .orElseThrow();


        return requestRepository
                .findByRequestedByIdOrderByRequestedAtDesc(
                        user.getId(),
                        PageRequest.of(page, 20)
                );
    }


    // =========================================================
    // GET PENDING REQUESTS
    // =========================================================

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


    // =========================================================
    // GET REQUEST
    // =========================================================

    @Transactional(readOnly = true)
    public EmployeeRequest getRequest(Long id) {

        return requestRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Request not found"
                        )
                );
    }


    // =========================================================
    // SEARCH EMPLOYEES WITHOUT PENDING REQUEST
    // (from origin/main)
    // =========================================================

    @Transactional(readOnly = true)
    public Page<Employee> searchEmployeesWithoutPendingRequest(
            String query,
            int page
    ) {

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
                .filter(employee ->
                        !pendingEmployeeIds.contains(employee.getId()))
                .toList();


        return new PageImpl<>(
                filtered,
                employees.getPageable(),
                filtered.size()
        );
    }


    // =========================================================
    // DGM DECISION
    // =========================================================

    @Transactional
    public void dgmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {

        EmployeeRequest request =
                requireStatus(
                        id,
                        RequestStatus.PENDING_DGM
                );


        User actor =
                userRepository.findByUsername(username)
                        .orElseThrow();


        ApprovalAction approvalAction =
                parseAction(action);


        saveHistory(
                request,
                actor,
                "DGM",
                approvalAction,
                remark
        );


        request.setStatus(
                approvalAction == ApprovalAction.APPROVED
                        ? RequestStatus.PENDING_GM
                        : RequestStatus.REJECTED
        );


        if (request.getStatus() ==
                RequestStatus.REJECTED) {

            deleteRejectedPendingImages(request);

            request.setCompletedAt(
                    LocalDateTime.now()
            );
        }
    }


    // =========================================================
    // GM DECISION
    // =========================================================

    @Transactional
    public void gmDecision(
            Long id,
            String action,
            String remark,
            String username
    ) {

        EmployeeRequest request =
                requireStatus(
                        id,
                        RequestStatus.PENDING_GM
                );


        User actor =
                userRepository.findByUsername(username)
                        .orElseThrow();


        ApprovalAction approvalAction =
                parseAction(action);


        saveHistory(
                request,
                actor,
                "GM",
                approvalAction,
                remark
        );


        if (approvalAction ==
                ApprovalAction.APPROVED) {


            Employee employee =
                    request.getTargetEmployee();


            if (employee == null) {

                if (employeeRepository
                        .existsByEmployeeNumber(
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

            employee.setPhotoPath(
                    request.getPhotoPath()
            );

            employee.setSignaturePath(
                    request.getSignaturePath()
            );

            employee.setSignatureValidFrom(
                    request.getSignatureValidFrom()
            );

            employee.setSignatureValidUntil(
                    request.getSignatureValidUntil()
            );


            employee =
                    employeeRepository.saveAndFlush(
                            employee
                    );


            String approvedPhotoPath =
                    fileStorageService.organizeEmployeeImage(
                            request.getPhotoPath(),
                            "profile",
                            employee.getId()
                    );


            String approvedSignaturePath =
                    fileStorageService.organizeEmployeeImage(
                            request.getSignaturePath(),
                            "signature",
                            employee.getId()
                    );


            employee.setPhotoPath(
                    approvedPhotoPath
            );

            employee.setSignaturePath(
                    approvedSignaturePath
            );

            request.setPhotoPath(
                    approvedPhotoPath
            );

            request.setSignaturePath(
                    approvedSignaturePath
            );


            employeeRepository.save(employee);


            EmployeeMediaVersion mediaVersion =
                    new EmployeeMediaVersion();

            mediaVersion.setEmployee(employee);

            mediaVersion.setRequest(request);

            mediaVersion.setVersionNumber(
                    (int) mediaVersionRepository
                            .countByEmployeeId(
                                    employee.getId()
                            ) + 1
            );

            mediaVersion.setPhotoPath(
                    approvedPhotoPath
            );

            mediaVersion.setSignaturePath(
                    approvedSignaturePath
            );

            mediaVersionRepository.save(
                    mediaVersion
            );


            request.setStatus(
                    RequestStatus.APPROVED
            );

        } else {

            deleteRejectedPendingImages(request);

            request.setStatus(
                    RequestStatus.REJECTED
            );
        }


        request.setCompletedAt(
                LocalDateTime.now()
        );
    }


    // =========================================================
    // REQUIRE STATUS
    // =========================================================

    private EmployeeRequest requireStatus(
            Long id,
            RequestStatus expected
    ) {

        EmployeeRequest request =
                requestRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Request not found"
                                )
                        );


        if (request.getStatus() != expected) {

            throw new IllegalArgumentException(
                    "Request is no longer pending at this approval level"
            );
        }


        return request;
    }


    // =========================================================
    // PARSE ACTION
    // =========================================================

    private ApprovalAction parseAction(
            String action
    ) {

        if ("approve".equals(action)) {
            return ApprovalAction.APPROVED;
        }

        if ("reject".equals(action)) {
            return ApprovalAction.REJECTED;
        }

        throw new IllegalArgumentException(
                "Invalid approval action"
        );
    }


    // =========================================================
    // DELETE REJECTED IMAGES
    // =========================================================

    private void deleteRejectedPendingImages(
            EmployeeRequest request
    ) {

        fileStorageService.deletePendingImage(
                request.getPhotoPath()
        );

        fileStorageService.deletePendingImage(
                request.getSignaturePath()
        );
    }


    // =========================================================
    // SAVE APPROVAL HISTORY
    // =========================================================

    private void saveHistory(
            EmployeeRequest request,
            User actor,
            String level,
            ApprovalAction action,
            String remark
    ) {

        if (remark == null ||
                remark.isBlank()) {

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

        history.setRemark(
                remark.trim()
        );

        approvalRepository.save(history);
    }
}