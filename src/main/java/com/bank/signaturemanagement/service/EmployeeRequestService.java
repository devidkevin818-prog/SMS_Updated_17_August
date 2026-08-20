package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.*;
import com.bank.signaturemanagement.repository.*;
import org.springframework.data.domain.Page;
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


        String code = form.getEmployeeCode().trim();


        if (employeeRepository.existsByEmployeeNumber(code)) {

            throw new IllegalArgumentException(
                    "Employee code already exists"
            );
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


        // =====================================================
        // Validate Images
        // =====================================================

        fileStorageService.validateImage(form.getPhoto());
        fileStorageService.validateImage(form.getSignature());


        // =====================================================
        // Find Designation / Department / Branch
        // =====================================================

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


        // =====================================================
        // Create Request
        // =====================================================

        EmployeeRequest request =
                new EmployeeRequest();


        request.setEmployeeCode(code);

        request.setEmployeeName(
                form.getEmployeeName().trim()
        );


        request.setDesignation(designation);

        request.setDepartment(department);

        request.setBranch(branch);


        request.setRemark(
                form.getRemark().trim()
        );


        // =====================================================
        // Store Images
        // =====================================================

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


        // =====================================================
        // Signature Validity
        // =====================================================

        request.setSignatureValidFrom(
                form.getSignatureValidFrom()
        );

        request.setSignatureValidUntil(
                form.getSignatureValidUntil()
        );


        // =====================================================
        // Requested By
        // =====================================================

        request.setRequestedBy(requester);


        // =====================================================
        // Save
        // =====================================================

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

        if (form.getSignatureValidFrom() == null ||
                form.getSignatureValidUntil() == null) {

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


        // =====================================================
        // Find Employee
        // =====================================================

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


        // =====================================================
        // Employee Code Validation
        // =====================================================

        String code =
                form.getEmployeeCode().trim();


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


        // =====================================================
        // Requester
        // =====================================================

        User requester =
                userRepository.findByUsername(username)
                        .orElseThrow();


        // =====================================================
        // Existing Images
        // =====================================================

        String photoPath =
                employee.getPhotoPath();

        String signaturePath =
                employee.getSignaturePath();


        // =====================================================
        // New Photo
        // =====================================================

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


        // =====================================================
        // New Signature
        // =====================================================

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


        // =====================================================
        // Find Designation / Department / Branch
        // =====================================================

        Designation designation = designationRepository
                .findById(form.getDesignationId())
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));

        Department department = departmentRepository
                .findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        Branch branch = branchRepository
                .findById(form.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));


        // =====================================================
        // Create Update Request
        // =====================================================

        EmployeeRequest request =
                new EmployeeRequest();


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


        // =====================================================
        // Save
        // =====================================================

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


            // =================================================
            // New Employee
            // =================================================

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

            }

            // =================================================
            // Existing Employee
            // =================================================

            else if (
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


            // =================================================
            // Employee Basic Information
            // =================================================

            employee.setEmployeeNumber(
                    request.getEmployeeCode()
            );


            employee.setFullName(
                    request.getEmployeeName()
            );


            // =================================================
            // Designation / Department / Branch
            // =================================================

            employee.setDesignation(
                    request.getDesignation()
            );


            employee.setDepartment(
                    request.getDepartment()
            );


            employee.setBranch(
                    request.getBranch()
            );


            // =================================================
            // Images
            // =================================================

            employee.setPhotoPath(
                    request.getPhotoPath()
            );


            employee.setSignaturePath(
                    request.getSignaturePath()
            );


            // =================================================
            // Signature Validity
            // =================================================

            employee.setSignatureValidFrom(
                    request.getSignatureValidFrom()
            );


            employee.setSignatureValidUntil(
                    request.getSignatureValidUntil()
            );


            // =================================================
            // Save Employee
            // =================================================

            employee =
                    employeeRepository.saveAndFlush(
                            employee
                    );


            // =================================================
            // Organize Approved Images
            // =================================================

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


            // =================================================
            // Update Employee Paths
            // =================================================

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


            // =================================================
            // Media Version
            // =================================================

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


            // =================================================
            // Request Approved
            // =================================================

            request.setStatus(
                    RequestStatus.APPROVED
            );

        } else {

            // =================================================
            // GM Rejected
            // =================================================

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