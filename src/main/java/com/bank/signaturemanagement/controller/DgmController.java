package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.EmployeeSerialNumberService;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.UserApprovalService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/dgm")
public class DgmController {

    private final EmployeeRequestService requestService;
    private final ApprovalHistoryService approvalHistoryService;
    private final EmployeeService employeeService;
    private final UserApprovalService userApprovalService;

    private final com.bank.signaturemanagement.service.EmployeeChangeProposalService
            changeProposalService;

    private final com.bank.signaturemanagement.service.BatchImportService
            batchImportService;

    private final com.bank.signaturemanagement.service.SignatureWorkflowService
            signatureWorkflowService;

    private final com.bank.signaturemanagement.service.EmployeeMediaRequestService
            mediaRequestService;

    private final com.bank.signaturemanagement.service.DashboardService
            dashboardService;

    private final EmployeeSerialNumberService employeeSerialNumberService;

    public DgmController(
            EmployeeRequestService requestService,
            ApprovalHistoryService approvalHistoryService,
            EmployeeService employeeService,
            UserApprovalService userApprovalService,
            com.bank.signaturemanagement.service.EmployeeChangeProposalService
                    changeProposalService,
            com.bank.signaturemanagement.service.BatchImportService
                    batchImportService,
            com.bank.signaturemanagement.service.SignatureWorkflowService
                    signatureWorkflowService,
            com.bank.signaturemanagement.service.EmployeeMediaRequestService
                    mediaRequestService,
            com.bank.signaturemanagement.service.DashboardService
                    dashboardService,
            EmployeeSerialNumberService employeeSerialNumberService
    ) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
        this.employeeService = employeeService;
        this.userApprovalService = userApprovalService;
        this.changeProposalService = changeProposalService;
        this.batchImportService = batchImportService;
        this.signatureWorkflowService = signatureWorkflowService;
        this.mediaRequestService = mediaRequestService;
        this.dashboardService = dashboardService;
        this.employeeSerialNumberService = employeeSerialNumberService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "dashboard",
                dashboardService.getDashboardData(
                        authentication.getName(),
                        "LEVEL_1_CHECKER"
                )
        );

        model.addAttribute(
                "requests",
                requestService.getPendingRequests(
                        RequestStatus.PENDING_DGM,
                        page
                )
        );

        model.addAttribute(
                "userRequests",
                userApprovalService.pending("LEVEL_1_CHECKER")
        );

        model.addAttribute(
                "batchRequests",
                batchImportService.pending("LEVEL_1_CHECKER")
        );

        model.addAttribute(
                "signatureRequests",
                signatureWorkflowService.pending("LEVEL_1_CHECKER")
        );

        model.addAttribute(
                "mediaRequests",
                mediaRequestService.pending("LEVEL_1_CHECKER")
        );

        return "dgm/dashboard";
    }

    @PostMapping("/batch-requests/{id}/decision")
    public String batchDecision(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            batchImportService.decide(
                    id,
                    "LEVEL_1_CHECKER",
                    action,
                    comment,
                    authentication.getName()
            );

            redirect.addFlashAttribute(
                    "success",
                    "Batch decision saved"
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/dashboard";
    }

    @GetMapping("/batch-requests/{id}")
    public String batchView(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute(
                "batch",
                batchImportService.get(id)
        );

        model.addAttribute(
                "items",
                batchImportService.itemViews(id)
        );

        model.addAttribute(
                "batchBase",
                "/dgm/dashboard"
        );

        model.addAttribute(
                "pageRole",
                "LEVEL_1_CHECKER"
        );

        model.addAttribute(
                "batchReadOnly",
                true
        );

        return "batches/detail";
    }

    @PostMapping("/user-requests/{id}/decision")
    public String userDecision(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String comment,
            Authentication authentication,
            RedirectAttributes redirect
    ) {
        try {
            userApprovalService.decide(
                    id,
                    "LEVEL_1_CHECKER",
                    action,
                    comment,
                    authentication.getName()
            );

            redirect.addFlashAttribute(
                    "success",
                    "User request decision saved"
            );
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/dashboard";
    }

    @GetMapping("/requests/{id}")
    public String review(
            @PathVariable Long id,
            Model model
    ) {
        EmployeeRequest request =
                requestService.getRequest(id);

        model.addAttribute(
                "request",
                request
        );

        model.addAttribute(
                "approvalForm",
                new ApprovalForm()
        );

        Employee employee =
                request.getTargetEmployee();

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

        return "dgm/request-review";
    }

    @PostMapping("/requests/{id}/decision")
    public String decide(
            @PathVariable Long id,
            @RequestParam String action,
            @ModelAttribute ApprovalForm approvalForm,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            requestService.dgmDecision(
                    id,
                    action,
                    approvalForm.getRemark(),
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Level 1 Checker decision saved"
            );

            return "redirect:/dgm/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

            return "redirect:/dgm/requests/" + id;
        }
    }

    @GetMapping("/approvals")
    public String approvals(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "approvals",
                approvalHistoryService.getDecisions(
                        authentication.getName(),
                        "LEVEL_1_CHECKER",
                        page
                )
        );

        return "dgm/approval-history";
    }

    @GetMapping("/employees")
    public String employees(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        var employeePage =
                requestService.searchEmployeesWithoutPendingRequest(
                        query,
                        page
                );

        model.addAttribute(
                "employees",
                employeePage
        );

        model.addAttribute(
                "query",
                query
        );

        addLatestSerialNumbers(
                model,
                employeePage.getContent()
        );

        return "dgm/employee-list";
    }

    @PostMapping("/employees/{id}/update-request")
    public String requestEmployeeUpdate(
            @PathVariable Long id,
            @RequestParam String justification,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            changeProposalService.submit(
                    id,
                    justification,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Update request submitted successfully"
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/employees";
    }

    /*
     * Adds the latest serial-number history record
     * for a single employee.
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

    /*
     * Adds the latest serial-number history records
     * for a collection of employees.
     */
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
}
