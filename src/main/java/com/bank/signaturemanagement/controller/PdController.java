package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import com.bank.signaturemanagement.repository.EmployeeRequestRepository;
import com.bank.signaturemanagement.repository.EmployeeStatusRepository;
import com.bank.signaturemanagement.service.ApprovedSignaturePdfService;
import com.bank.signaturemanagement.service.BranchService;
import com.bank.signaturemanagement.service.DashboardService;
import com.bank.signaturemanagement.service.DepartmentService;
import com.bank.signaturemanagement.service.DesignationService;
import com.bank.signaturemanagement.service.EmployeeChangeProposalService;
import com.bank.signaturemanagement.service.EmployeeNumberFormat;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.EmployeeSerialNumberService;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.UserApprovalService;
import com.bank.signaturemanagement.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.bank.signaturemanagement.service.EmployeeRequestService.PENDING_STATUSES;

@Controller
@RequestMapping("/pd")
public class PdController {

    private final EmployeeStatusRepository employeeStatusRepository;
    private final EmployeeRequestRepository requestRepository;
    private final EmployeeRequestService requestService;
    private final EmployeeService employeeService;
    private final ApprovedSignaturePdfService pdfService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;
    private final DesignationService designationService;
    private final DepartmentService departmentService;
    private final BranchService branchService;
    private final UserService userService;
    private final UserApprovalService userApprovalService;
    private final EmployeeChangeProposalService changeProposalService;
    private final DashboardService dashboardService;
    private final EmployeeSerialNumberService employeeSerialNumberService;

    public PdController(
            EmployeeStatusRepository employeeStatusRepository,
            EmployeeRequestRepository requestRepository,
            EmployeeRequestService requestService,
            EmployeeService employeeService,
            ApprovedSignaturePdfService pdfService,
            EmployeeMediaVersionRepository mediaVersionRepository,
            DesignationService designationService,
            DepartmentService departmentService,
            BranchService branchService,
            UserService userService,
            UserApprovalService userApprovalService,
            EmployeeChangeProposalService changeProposalService,
            DashboardService dashboardService,
            EmployeeSerialNumberService employeeSerialNumberService
    ) {
        this.employeeStatusRepository = employeeStatusRepository;
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.employeeService = employeeService;
        this.pdfService = pdfService;
        this.mediaVersionRepository = mediaVersionRepository;
        this.designationService = designationService;
        this.departmentService = departmentService;
        this.branchService = branchService;
        this.userService = userService;
        this.userApprovalService = userApprovalService;
        this.changeProposalService = changeProposalService;
        this.dashboardService = dashboardService;
        this.employeeSerialNumberService = employeeSerialNumberService;
    }

    /*
     * User creation
     */

    @GetMapping("/users/new")
    public String createUserForm(Model model) {
        model.addAttribute("userForm", new UserForm());

        addUserReferenceData(model);

        return "admin/create-user";
    }

    @PostMapping("/users")
    public String createUser(
            @Valid
            @ModelAttribute("userForm")
            UserForm form,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            addUserReferenceData(model);

            return "admin/create-user";
        }

        try {
            userApprovalService.propose(
                    form,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "User request submitted for DGM approval"
            );

            return "redirect:/pd/dashboard";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject(
                    "user",
                    exception.getMessage()
            );

            addUserReferenceData(model);

            return "admin/create-user";
        }
    }

    private void addUserReferenceData(Model model) {
        model.addAttribute(
                "branches",
                userService.getBranches()
        );

        model.addAttribute(
                "roles",
                userService.getRoles()
                        .stream()
                        .filter(role ->
                                !"ADMIN".equals(role.getName())
                        )
                        .toList()
        );

        model.addAttribute(
                "creatorRole",
                "PD"
        );

        model.addAttribute(
                "creatorBackPath",
                "/pd/dashboard"
        );

        model.addAttribute(
                "userCreateAction",
                "/pd/users"
        );
    }

    /*
     * Dashboard
     */

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                dashboardService.getDashboardData(
                        authentication.getName(),
                        "PD"
                )
        );

        var changeProposals =
                changeProposalService.pendingPd(
                        authentication.getName()
                );

        model.addAttribute(
                "changeProposals",
                changeProposals
        );

        model.addAttribute(
                "changeProposalCount",
                changeProposals.size()
        );

        model.addAttribute(
                "myRequestCount",
                requestService
                        .getRequestsForUser(
                                authentication.getName(),
                                0
                        )
                        .getTotalElements()
        );

        return "pd/dashboard";
    }

    /*
     * Employee change proposals
     */

    @PostMapping("/employees/{id}/toggle-lock")
    public String toggleLock(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        changeProposalService.toggleLock(
                id,
                authentication.getName()
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Employee edit lock updated"
        );

        return "redirect:/pd/employees";
    }

    @PostMapping("/change-proposals/{id}/accept")
    public String acceptProposal(
            @PathVariable Long id,
            Authentication authentication
    ) {
        var proposal =
                changeProposalService.acceptForEditing(
                        id,
                        authentication.getName()
                );

        return "redirect:/pd/employees/"
                + proposal.getEmployee().getId()
                + "/edit?proposalId="
                + proposal.getId();
    }

    /*
     * Create employee
     */

    @GetMapping("/employees/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("employeeRequestForm")) {
            EmployeeRequestForm form =
                    new EmployeeRequestForm();

            employeeStatusRepository
                    .findByActiveTrueOrderByDisplayOrderAscStatusNameAsc()
                    .stream()
                    .findFirst()
                    .ifPresent(status ->
                            form.setStatusId(status.getStatusId())
                    );

            model.addAttribute(
                    "employeeRequestForm",
                    form
            );
        }

        addReferenceData(model);

        return "pd/create-employee";
    }

    @PostMapping("/employees")
    public String create(
            @Valid
            @ModelAttribute("employeeRequestForm")
            EmployeeRequestForm employeeRequestForm,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (result.hasErrors()) {
            addReferenceData(model);

            return "pd/create-employee";
        }

        try {
            requestService.createRequest(
                    employeeRequestForm,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Employee request submitted to DGM"
            );

            return "redirect:/pd/requests";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject(
                    "request",
                    exception.getMessage()
            );

            addReferenceData(model);

            return "pd/create-employee";
        }
    }

    /*
     * Request list
     */

    @GetMapping("/requests")
    public String requests(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "requests",
                requestService.getRequestsForUser(
                        authentication.getName(),
                        page
                )
        );

        return "pd/request-list";
    }

    /*
     * Employee list
     */

    @GetMapping("/employees")
    public String employees(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        var employeePage =
                employeeService.search(query, page);

        model.addAttribute(
                "query",
                query
        );

        model.addAttribute(
                "employees",
                employeePage
        );

        addLatestSerialNumbers(
                model,
                employeePage.getContent()
        );

        return "pd/employee-list";
    }

    /*
     * Approved signatures
     */

    @GetMapping("/approved-signatures")
    public String approvedSignatures(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        var employeePage =
                employeeService.search(query, page);

        model.addAttribute(
                "query",
                query
        );

        model.addAttribute(
                "employees",
                employeePage
        );

        addLatestSerialNumbers(
                model,
                employeePage.getContent()
        );

        return "pd/approved-signatures";
    }

    @GetMapping("/approved-signatures/pdf")
    public void downloadApprovedPdf(
            HttpServletResponse response
    ) throws Exception {
        response.setContentType("application/pdf");

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"approved-signatures.pdf\""
        );

        pdfService.generateApprovedPdf(
                response.getOutputStream()
        );
    }

    @GetMapping("/approved-signatures/{id}")
    public String approvedSignatureVersions(
            @PathVariable Long id,
            Model model
    ) {
        Employee employee =
                employeeService.getEmployee(id);

        model.addAttribute(
                "employee",
                employee
        );

        addLatestSerialNumber(
                model,
                id
        );

        model.addAttribute(
                "versions",
                mediaVersionRepository
                        .findByEmployeeIdOrderByVersionNumberDesc(id)
        );

        return "pd/approved-signature-versions";
    }

    /*
     * Edit employee
     */

    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(
            @PathVariable Long id,
            @RequestParam(required = false)
            Long rejectedRequestId,
            @RequestParam(required = false)
            Long proposalId,
            Authentication authentication,
            Model model
    ) {
        /*
         * Enable this requirement if edits must always originate from
         * a DGM or GM change proposal.
         *
         * if (proposalId == null) {
         *     throw new IllegalStateException(
         *             "DGM or GM must initiate this employee update first"
         *     );
         * }
         *
         * changeProposalService.requireEditing(
         *         proposalId,
         *         id,
         *         authentication.getName()
         * );
         */

        if (rejectedRequestId != null) {
            Long targetEmployeeId =
                    requestService.getTargetEmployeeIdForUpdate(
                            rejectedRequestId,
                            authentication.getName()
                    );

            if (!id.equals(targetEmployeeId)) {
                throw new IllegalArgumentException(
                        "Invalid employee update request"
                );
            }
        }

        Employee employee =
                employeeService.getEmployee(id);

        model.addAttribute(
                "employee",
                employee
        );

        addLatestSerialNumber(
                model,
                id
        );

        model.addAttribute(
                "employeeUpdateForm",
                employeeService.getUpdateForm(id)
        );

        model.addAttribute(
                "proposalId",
                proposalId
        );

        if (rejectedRequestId != null) {
            model.addAttribute(
                    "rejectedRequestId",
                    rejectedRequestId
            );
        }

        addReferenceData(model);

        return "pd/edit-employee";
    }

    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(
            @PathVariable Long id,
            @RequestParam(required = false)
            Long rejectedRequestId,
            @RequestParam(required = false)
            Long proposalId,
            @Valid
            @ModelAttribute("employeeUpdateForm")
            EmployeeUpdateForm employeeUpdateForm,
            BindingResult result,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            prepareEmployeeEditPage(
                    id,
                    rejectedRequestId,
                    proposalId,
                    model
            );

            return "pd/edit-employee";
        }

        try {
            /*
             * The path-variable ID is the Employee entity's database ID.
             * Do not convert employeeCode to Long for this query.
             */
            boolean pendingRequestExists =
                    requestRepository
                            .existsByTargetEmployeeIdAndStatusIn(
                                    id,
                                    PENDING_STATUSES
                            );

            if (pendingRequestExists) {
                throw new IllegalStateException(
                        "A pending update request already exists "
                                + "for this employee"
                );
            }

            /*
             * If proposal-based editing is mandatory, restore:
             *
             * var proposal =
             *         changeProposalService.requireEditing(
             *                 proposalId,
             *                 id,
             *                 authentication.getName()
             *         );
             *
             * Then pass proposal to the appropriate service method.
             */

            requestService.createUpdateRequest(
                    id,
                    employeeUpdateForm,
                    authentication.getName()
            );

            /*
             * This assumes true means that an update request
             * is currently pending.
             */
            employeeService.updateRequestStatus(
                    id,
                    true
            );

            if (rejectedRequestId != null) {
                requestService.markUpdateRequestCompleted(
                        rejectedRequestId
                );
            }

            if (proposalId != null) {
                changeProposalService.markSubmitted(
                        proposalId,
                        authentication.getName()
                );
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Employee update submitted to DGM for approval"
            );

            return "redirect:/pd/requests";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject(
                    "employee",
                    exception.getMessage()
            );

            prepareEmployeeEditPage(
                    id,
                    rejectedRequestId,
                    proposalId,
                    model
            );

            return "pd/edit-employee";
        }
    }

    private void prepareEmployeeEditPage(
            Long employeeId,
            Long rejectedRequestId,
            Long proposalId,
            Model model
    ) {
        model.addAttribute(
                "employee",
                employeeService.getEmployee(employeeId)
        );

        addLatestSerialNumber(
                model,
                employeeId
        );

        model.addAttribute(
                "proposalId",
                proposalId
        );

        if (rejectedRequestId != null) {
            model.addAttribute(
                    "rejectedRequestId",
                    rejectedRequestId
            );
        }

        addReferenceData(model);
    }

    /*
     * Update and resubmit rejected request
     */

    @GetMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        EmployeeRequest request =
                requestService.getRequest(id);

        requireOriginalRequester(
                request,
                authentication.getName()
        );

        Employee employee =
                request.getTargetEmployee();

        if (employee == null) {
            throw new IllegalStateException(
                    "This request is not linked to an existing employee"
            );
        }

        request.setEmployeeCode(
                EmployeeNumberFormat.editablePart(
                        request.getEmployeeCode()
                )
        );

        request.setRemark("");

        model.addAttribute(
                "request",
                request
        );

        model.addAttribute(
                "employee",
                employee
        );

        addLatestSerialNumber(
                model,
                employee.getId()
        );

        return "pd/update-request";
    }

    @PostMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("request")
            EmployeeRequest updatedRequest,
            BindingResult result,
            @RequestParam(
                    value = "foreignSignature",
                    required = false
            )
            MultipartFile foreignSignature,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        EmployeeRequest existingRequest =
                requestService.getRequest(id);

        requireOriginalRequester(
                existingRequest,
                authentication.getName()
        );

        preserveFilePaths(
                updatedRequest,
                existingRequest
        );

        Employee targetEmployee =
                existingRequest.getTargetEmployee();

        if (result.hasErrors()) {
            prepareRejectedRequestPage(
                    model,
                    updatedRequest,
                    targetEmployee
            );

            return "pd/update-request";
        }

        try {
            requestService.updateRequest(
                    id,
                    updatedRequest,
                    foreignSignature,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Rejected request updated and resubmitted to DGM"
            );

            return "redirect:/pd/requests";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject(
                    "request",
                    exception.getMessage()
            );

            prepareRejectedRequestPage(
                    model,
                    updatedRequest,
                    targetEmployee
            );

            return "pd/update-request";
        }
    }

    @GetMapping("/request/{id}/edit")
    public String editEmployeeRequest(
            @PathVariable("id") Long id,
            Authentication authentication,
            Model model
    ) {
        EmployeeRequest request =
                requestService.getRequest(id);

        requireOriginalRequester(
                request,
                authentication.getName()
        );

        Employee employee =
                request.getTargetEmployee();

        model.addAttribute(
                "request",
                request
        );

        model.addAttribute(
                "employee",
                employee
        );

        if (employee != null) {
            addLatestSerialNumber(
                    model,
                    employee.getId()
            );
        } else {
            model.addAttribute(
                    "employeeSerialNumber",
                    null
            );
        }

        return "pd/update-request";
    }

    /*
     * Serial-number model helper methods
     */

    private void addLatestSerialNumber(
            Model model,
            Long employeeId
    ) {
        EmployeeSerialNumber serialNumber =
                employeeSerialNumberService
                        .findLatestByEmployeeId(employeeId)
                        .orElse(null);

        model.addAttribute(
                "employeeSerialNumber",
                serialNumber
        );
    }

    private void addLatestSerialNumbers(
            Model model,
            Iterable<Employee> employees
    ) {
        Map<Long, EmployeeSerialNumber> serialNumbersByEmployeeId =
                new LinkedHashMap<>();

        for (Employee employee : employees) {
            EmployeeSerialNumber serialNumber =
                    employeeSerialNumberService
                            .findLatestByEmployeeId(
                                    employee.getId()
                            )
                            .orElse(null);

            serialNumbersByEmployeeId.put(
                    employee.getId(),
                    serialNumber
            );
        }

        model.addAttribute(
                "serialNumbersByEmployeeId",
                serialNumbersByEmployeeId
        );
    }

    /*
     * Shared page preparation methods
     */

    private void prepareRejectedRequestPage(
            Model model,
            EmployeeRequest request,
            Employee employee
    ) {
        model.addAttribute(
                "request",
                request
        );

        model.addAttribute(
                "employee",
                employee
        );

        if (employee != null) {
            addLatestSerialNumber(
                    model,
                    employee.getId()
            );
        } else {
            model.addAttribute(
                    "employeeSerialNumber",
                    null
            );
        }
    }

    private void addReferenceData(Model model) {
        model.addAttribute(
                "designations",
                designationService.findAll()
        );

        model.addAttribute(
                "departments",
                departmentService.findAll()
        );

        model.addAttribute(
                "branches",
                branchService.findAll()
        );

        model.addAttribute(
                "employeeStatuses",
                employeeStatusRepository
                        .findByActiveTrueOrderByDisplayOrderAscStatusNameAsc()
        );
    }

    private void requireOriginalRequester(
            EmployeeRequest request,
            String username
    ) {
        if (request.getRequestedBy() == null
                || request.getRequestedBy().getUsername() == null
                || !request.getRequestedBy()
                .getUsername()
                .equals(username)) {

            throw new IllegalArgumentException(
                    "You are not authorized to update this request"
            );
        }
    }

    private void preserveFilePaths(
            EmployeeRequest updated,
            EmployeeRequest existing
    ) {
        if (updated.getPhotoPath() == null
                || updated.getPhotoPath().isBlank()) {

            updated.setPhotoPath(
                    existing.getPhotoPath()
            );
        }

        if (updated.getSignaturePath() == null
                || updated.getSignaturePath().isBlank()) {

            updated.setSignaturePath(
                    existing.getSignaturePath()
            );
        }

        if (updated.getForeignSignaturePath() == null
                || updated.getForeignSignaturePath().isBlank()) {

            updated.setForeignSignaturePath(
                    existing.getForeignSignaturePath()
            );
        }
    }
}
