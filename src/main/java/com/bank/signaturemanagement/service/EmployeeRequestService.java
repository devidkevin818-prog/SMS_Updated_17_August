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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeRequestService {
    private static final List<RequestStatus> PENDING_STATUSES =
            List.of(RequestStatus.PENDING_DGM, RequestStatus.PENDING_GM);

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
            BranchRepository branchRepository) {
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

    @Transactional
    public void createRequest(EmployeeRequestForm form, String username) {
        validateDates(form.getSignatureValidFrom(), form.getSignatureValidUntil());
        String code = EmployeeNumberFormat.normalize(form.getEmployeeCode());
        validateEmployeeCodeAvailable(code, null);

        fileStorageService.validateImage(form.getPhoto());
        fileStorageService.validateImage(form.getSignature());

        EmployeeRequest request = new EmployeeRequest();
        request.setRequestedBy(requireUser(username));
        request.setEmployeeCode(code);
        request.setEmployeeName(form.getEmployeeName().trim());
        request.setDesignation(requireDesignation(form.getDesignation()));
        request.setDepartment(requireDepartment(form.getDepartment()));
        request.setBranch(requireBranch(form.getBranch()));
        request.setRemark(form.getRemark().trim());
        request.setPhotoPath(fileStorageService.storeImage(form.getPhoto(), "employee-photo"));
        request.setSignaturePath(fileStorageService.storeImage(form.getSignature(), "employee-signature"));
        request.setForeignSignaturePath(storeOptionalImage(form.getForeignSignature(), "pending-foreign-signature", null));
        request.setSignatureValidFrom(form.getSignatureValidFrom());
        request.setSignatureValidUntil(form.getSignatureValidUntil());
        requestRepository.save(request);
    }

    @Transactional
    public void createUpdateRequest(Long employeeId, EmployeeUpdateForm form, String username) {
        validateDates(form.getSignatureValidFrom(), form.getSignatureValidUntil());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (requestRepository.existsByTargetEmployeeIdAndStatusIn(employeeId, PENDING_STATUSES)) {
            throw new IllegalArgumentException("A pending update request already exists for this employee");
        }

        String code = EmployeeNumberFormat.normalize(form.getEmployeeCode());
        validateEmployeeCodeAvailable(code, employeeId);

        EmployeeRequest request = new EmployeeRequest();
        request.setRequestedBy(requireUser(username));
        request.setTargetEmployee(employee);
        request.setEmployeeCode(code);
        request.setEmployeeName(form.getEmployeeName().trim());
        request.setDesignation(requireDesignation(form.getDesignationId()));
        request.setDepartment(requireDepartment(form.getDepartmentId()));
        request.setBranch(requireBranch(form.getBranchId()));
        request.setPhotoPath(storeOptionalImage(form.getPhoto(), "pending-photo", employee.getPhotoPath()));
        request.setSignaturePath(storeOptionalImage(form.getSignature(), "pending-signature", employee.getSignaturePath()));
        request.setForeignSignaturePath(storeOptionalImage(
                form.getForeignSignature(), "pending-foreign-signature", employee.getForeignSignaturePath()));
        request.setRemark(form.getRemark().trim());
        request.setSignatureValidFrom(form.getSignatureValidFrom());
        request.setSignatureValidUntil(form.getSignatureValidUntil());
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getRequestsForUser(String username, int page) {
        User user = requireUser(username);
        return requestRepository.findByRequestedByIdOrderByRequestedAtDesc(
                user.getId(), PageRequest.of(page, 20));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeRequest> getPendingRequests(RequestStatus status, int page) {
        return requestRepository.findByStatusOrderByRequestedAtAsc(status, PageRequest.of(page, 20));
    }

    @Transactional(readOnly = true)
    public EmployeeRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    @Transactional
    public void dgmDecision(Long id, String action, String remark, String username) {
        EmployeeRequest request = requireStatus(id, RequestStatus.PENDING_DGM);
        ApprovalAction decision = parseAction(action);
        saveHistory(request, requireUser(username), "DGM", decision, remark);
        request.setStatus(decision == ApprovalAction.APPROVED
                ? RequestStatus.PENDING_GM : RequestStatus.REJECTED);
        if (decision == ApprovalAction.REJECTED) {
            markRejected(request);
        }
    }

    @Transactional
    public void gmDecision(Long id, String action, String remark, String username) {
        EmployeeRequest request = requireStatus(id, RequestStatus.PENDING_GM);
        ApprovalAction decision = parseAction(action);
        saveHistory(request, requireUser(username), "GM", decision, remark);

        if (decision == ApprovalAction.REJECTED) {
            request.setStatus(RequestStatus.REJECTED);
            markRejected(request);
            return;
        }

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

        applyApprovedRequest(employee, request);
        employee = employeeRepository.saveAndFlush(employee);
        organizeApprovedImages(employee, request);
        employeeRepository.save(employee);
        saveMediaVersion(employee, request);

        request.setStatus(RequestStatus.APPROVED);
        request.setUpdateRequestStatus(false);
        request.setCompletedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Long getTargetEmployeeIdForUpdate(Long requestId, String username) {
        EmployeeRequest request = getRequest(requestId);
        requireOriginalRequester(request, username);
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
        getRequest(requestId).setUpdateRequestStatus(false);
    }

    @Transactional
    public void updateRequest(Long requestId, EmployeeRequest updatedRequest, String username) {
        updateRequest(requestId, updatedRequest, null, username);
    }

    @Transactional
    public void updateRequest(
            Long requestId,
            EmployeeRequest updatedRequest,
            MultipartFile foreignSignature,
            String username) {
        EmployeeRequest request = getRequest(requestId);
        requireOriginalRequester(request, username);
        if (request.getStatus() != RequestStatus.REJECTED) {
            throw new IllegalStateException("Only rejected requests can be updated");
        }

        validateDates(updatedRequest.getSignatureValidFrom(), updatedRequest.getSignatureValidUntil());
        String code = EmployeeNumberFormat.normalize(updatedRequest.getEmployeeCode());
        Long targetId = request.getTargetEmployee() == null ? null : request.getTargetEmployee().getId();
        validateEmployeeCodeAvailable(code, targetId);

        request.setEmployeeCode(code);
        request.setEmployeeName(updatedRequest.getEmployeeName().trim());
        request.setDesignation(requireEntity(updatedRequest.getDesignation(), "Designation is required"));
        request.setDepartment(requireEntity(updatedRequest.getDepartment(), "Department is required"));
        request.setBranch(requireEntity(updatedRequest.getBranch(), "Branch is required"));
        request.setSignatureValidFrom(updatedRequest.getSignatureValidFrom());
        request.setSignatureValidUntil(updatedRequest.getSignatureValidUntil());
        request.setRemark(updatedRequest.getRemark().trim());
        request.setForeignSignaturePath(storeOptionalImage(
                foreignSignature, "pending-foreign-signature", request.getForeignSignaturePath()));
        request.setUpdatedAfterRejection(true);
        request.setUpdateRequestStatus(false);
        request.setStatus(RequestStatus.PENDING_DGM);
        request.setCompletedAt(null);
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<Employee> searchEmployeesWithoutPendingRequest(String query, int page) {
        String text = query == null ? "" : query.trim();
        List<Long> pendingEmployeeIds = requestRepository.findByStatusIn(PENDING_STATUSES).stream()
                .map(EmployeeRequest::getTargetEmployee)
                .filter(employee -> employee != null)
                .map(Employee::getId)
                .toList();
        Page<Employee> employees = employeeRepository
                .findByEmployeeNumberContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        text, text, PageRequest.of(page, 20));
        List<Employee> filtered = employees.getContent().stream()
                .filter(employee -> !pendingEmployeeIds.contains(employee.getId()))
                .toList();
        return new PageImpl<>(filtered, employees.getPageable(), filtered.size());
    }

    private void validateEmployeeCodeAvailable(String code, Long targetEmployeeId) {
        boolean employeeExists = targetEmployeeId == null
                ? employeeRepository.existsByEmployeeNumber(code)
                : employeeRepository.existsByEmployeeNumberAndIdNot(code, targetEmployeeId);
        if (employeeExists) {
            throw new IllegalArgumentException("Employee code already exists");
        }
        if (requestRepository.existsByEmployeeCodeAndStatusIn(code, PENDING_STATUSES)) {
            throw new IllegalArgumentException("A pending request already exists for this employee code");
        }
    }

    private void validateDates(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom == null || validUntil == null) {
            throw new IllegalArgumentException("Signature validity dates are required");
        }
        if (validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "Signature valid-until date must be on or after the valid-from date.");
        }
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Designation requireDesignation(String name) {
        return designationRepository.findByDesignationName(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));
    }

    private Department requireDepartment(String name) {
        return departmentRepository.findByDepartmentName(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private Branch requireBranch(String name) {
        return branchRepository.findByBranchName(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }

    private Designation requireDesignation(Long id) {
        return designationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Designation not found"));
    }

    private Department requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private Branch requireBranch(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
    }

    private <T> T requireEntity(T entity, String message) {
        if (entity == null) {
            throw new IllegalArgumentException(message);
        }
        return entity;
    }

    private String storeOptionalImage(
            org.springframework.web.multipart.MultipartFile image,
            String folder,
            String existingPath) {
        if (image == null || image.isEmpty()) {
            return existingPath;
        }
        fileStorageService.validateImage(image);
        return fileStorageService.storeImage(image, folder);
    }

    private EmployeeRequest requireStatus(Long id, RequestStatus expected) {
        EmployeeRequest request = getRequest(id);
        if (request.getStatus() != expected) {
            throw new IllegalArgumentException("Request is no longer pending at this approval level");
        }
        return request;
    }

    private ApprovalAction parseAction(String action) {
        if ("approve".equalsIgnoreCase(action)) return ApprovalAction.APPROVED;
        if ("reject".equalsIgnoreCase(action)) return ApprovalAction.REJECTED;
        throw new IllegalArgumentException("Invalid approval action");
    }

    private void saveHistory(EmployeeRequest request, User actor, String level,
                             ApprovalAction action, String remark) {
        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException("Remark is required");
        }
        ApprovalHistory history = new ApprovalHistory();
        history.setRequest(request);
        history.setActedBy(actor);
        history.setApprovalLevel(level);
        history.setAction(action);
        history.setRemark(remark.trim());
        approvalRepository.save(history);
    }

    private void markRejected(EmployeeRequest request) {
        request.setUpdatedAfterRejection(false);
        request.setUpdateRequestStatus(true);
        request.setCompletedAt(LocalDateTime.now());
    }

    private void requireOriginalRequester(EmployeeRequest request, String username) {
        if (!request.getRequestedBy().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to update this request");
        }
    }

    private void applyApprovedRequest(Employee employee, EmployeeRequest request) {
        employee.setEmployeeNumber(request.getEmployeeCode());
        employee.setFullName(request.getEmployeeName());
        employee.setDesignation(request.getDesignation());
        employee.setDepartment(request.getDepartment());
        employee.setBranch(request.getBranch());
        employee.setPhotoPath(request.getPhotoPath());
        employee.setSignaturePath(request.getSignaturePath());
        employee.setForeignSignaturePath(request.getForeignSignaturePath());
        employee.setSignatureValidFrom(request.getSignatureValidFrom());
        employee.setSignatureValidUntil(request.getSignatureValidUntil());
    }

    private void organizeApprovedImages(Employee employee, EmployeeRequest request) {
        String photoPath = fileStorageService.organizeEmployeeImage(
                request.getPhotoPath(), "profile", employee.getId());
        String signaturePath = fileStorageService.organizeEmployeeImage(
                request.getSignaturePath(), "signature", employee.getId());
        String foreignSignaturePath = null;
        if (request.getForeignSignaturePath() != null && !request.getForeignSignaturePath().isBlank()) {
            foreignSignaturePath = fileStorageService.organizeEmployeeImage(
                    request.getForeignSignaturePath(), "foreign-signature", employee.getId());
        }
        employee.setPhotoPath(photoPath);
        employee.setSignaturePath(signaturePath);
        employee.setForeignSignaturePath(foreignSignaturePath);
        request.setPhotoPath(photoPath);
        request.setSignaturePath(signaturePath);
        request.setForeignSignaturePath(foreignSignaturePath);
    }

    private void saveMediaVersion(Employee employee, EmployeeRequest request) {
        EmployeeMediaVersion version = new EmployeeMediaVersion();
        version.setEmployee(employee);
        version.setRequest(request);
        version.setVersionNumber((int) mediaVersionRepository.countByEmployeeId(employee.getId()) + 1);
        version.setPhotoPath(employee.getPhotoPath());
        version.setSignaturePath(employee.getSignaturePath());
        version.setForeignSignaturePath(employee.getForeignSignaturePath());
        mediaVersionRepository.save(version);
    }
}
