package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
import com.bank.signaturemanagement.service.BatchImportService;
import com.bank.signaturemanagement.service.DashboardService;
import com.bank.signaturemanagement.service.EmployeeChangeProposalService;
import com.bank.signaturemanagement.service.EmployeeMediaRequestService;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.EmployeeSerialNumberService;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.SignatureWorkflowService;
import com.bank.signaturemanagement.service.UserApprovalService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/dgm")
public class DgmController {

    private static final String LEVEL_1_CHECKER =
            "LEVEL_1_CHECKER";

    private final EmployeeRequestService requestService;
    private final ApprovalHistoryService approvalHistoryService;
    private final EmployeeService employeeService;
    private final UserApprovalService userApprovalService;
    private final BatchImportService batchImportService;
    private final EmployeeChangeProposalService changeProposalService;
    private final SignatureWorkflowService signatureWorkflowService;
    private final EmployeeMediaRequestService mediaRequestService;
    private final DashboardService dashboardService;
    private final EmployeeSerialNumberService employeeSerialNumberService;

    public DgmController(
            EmployeeRequestService requestService,
            ApprovalHistoryService approvalHistoryService,
            EmployeeService employeeService,
            UserApprovalService userApprovalService,
            BatchImportService batchImportService,
            EmployeeChangeProposalService changeProposalService,
            SignatureWorkflowService signatureWorkflowService,
            EmployeeMediaRequestService mediaRequestService,
            DashboardService dashboardService,
            EmployeeSerialNumberService employeeSerialNumberService
    ) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
        this.employeeService = employeeService;
        this.userApprovalService = userApprovalService;
        this.batchImportService = batchImportService;
        this.changeProposalService = changeProposalService;
        this.signatureWorkflowService = signatureWorkflowService;
        this.mediaRequestService = mediaRequestService;
        this.dashboardService = dashboardService;
        this.employeeSerialNumberService = employeeSerialNumberService;
    }

    /*
     * ============================================================
     * LEVEL 1 CHECKER DASHBOARD
     * ============================================================
     */

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            ) int page,
            Authentication authentication,
            Model model
    ) {
        String username = authentication.getName();

        /*
         * Dashboard summary and analytics.
         */
        model.addAttribute(
                "dashboard",
                dashboardService.getDashboardData(
                        username,
                        LEVEL_1_CHECKER
                )
        );

        /*
         * Pending employee creation or update requests.
         */
        model.addAttribute(
                "requests",
                requestService.getPendingRequests(
                        RequestStatus.PENDING_DGM,
                        page
                )
        );

        /*
         * Load Level 1 Checker approval queues.
         */
        var userRequests =
                userApprovalService.pending(
                        LEVEL_1_CHECKER
                );

        var batchRequests =
                batchImportService.findPendingForLevel1Checker();

        var signatureRequests =
                signatureWorkflowService.pending(
                        LEVEL_1_CHECKER
                );

        var mediaRequests =
                mediaRequestService.pending(
                        LEVEL_1_CHECKER
                );

        model.addAttribute(
                "userRequests",
                userRequests == null
                        ? Collections.emptyList()
                        : userRequests
        );

        model.addAttribute(
                "batchRequests",
                batchRequests == null
                        ? Collections.emptyList()
                        : batchRequests
        );

        model.addAttribute(
                "signatureRequests",
                signatureRequests == null
                        ? Collections.emptyList()
                        : signatureRequests
        );

        model.addAttribute(
                "mediaRequests",
                mediaRequests == null
                        ? Collections.emptyList()
                        : mediaRequests
        );

        /*
         * Temporary diagnostics.
         * Remove after confirming that batch requests appear.
         */
        int batchRequestCount =
                batchRequests == null
                        ? 0
                        : batchRequests.size();

        System.out.println(
                "LEVEL_1_CHECKER batch request count = "
                        + batchRequestCount
        );

        if (batchRequests != null) {
            batchRequests.forEach(batch ->
                    System.out.println(
                            "Visible batch: id="
                                    + batch.getId()
                                    + ", batchNumber="
                                    + batch.getBatchNumber()
                                    + ", status="
                                    + batch.getStatus()
                                    + ", active="
                                    + batch.isActive()
                    )
            );
        }

        return "dgm/dashboard";
    }

    /*
     * ============================================================
     * IMPORT BATCH APPROVAL
     * ============================================================
     *
     * The GET endpoint for viewing a batch is intentionally not
     * declared here.
     *
     * BatchController already owns:
     *
     * GET /dgm/batch-requests/{id}
     *
     * Declaring the same route here causes an ambiguous mapping
     * and prevents the application from starting.
     * ============================================================
     */

//    @PostMapping("/batch-requests/{id}/decision")
//    public String batchDecision(
//            @PathVariable("id") Long id,
//            @RequestParam("action") String action,
//            @RequestParam(
//                    value = "comment",
//                    required = false
//            ) String comment,
//            RedirectAttributes redirectAttributes
//    ) {
//        try {
//            batchImportService.makeLevel1Decision(
//                    id,
//                    action,
//                    comment
//            );
//
//            String successMessage =
//                    "APPROVE".equalsIgnoreCase(action)
//                            ? "Batch approved and sent to the Level 2 Checker."
//                            : "Batch rejected successfully.";
//
//            redirectAttributes.addFlashAttribute(
//                    "success",
//                    successMessage
//            );
//        } catch (IllegalArgumentException
//                 | IllegalStateException exception) {
//
//            redirectAttributes.addFlashAttribute(
//                    "error",
//                    exception.getMessage()
//            );
//        }
//
//        return "redirect:/dgm/dashboard#batch-requests";
//    }

    /*
     * ============================================================
     * USER CREATION APPROVAL
     * ============================================================
     */

    @PostMapping("/user-requests/{id}/decision")
    public String userDecision(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            @RequestParam(
                    value = "comment",
                    required = false
            ) String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userApprovalService.decide(
                    id,
                    LEVEL_1_CHECKER,
                    action,
                    comment,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "User request decision saved."
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/dashboard";
    }

    /*
     * ============================================================
     * EMPLOYEE REQUEST APPROVAL
     * ============================================================
     */

    @GetMapping("/requests/{id}")
    public String review(
            @PathVariable("id") Long id,
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
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
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
                    "Level 1 Checker decision saved."
            );

            return "redirect:/dgm/dashboard";
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

            return "redirect:/dgm/requests/" + id;
        }
    }

    /*
     * ============================================================
     * APPROVAL HISTORY
     * ============================================================
     */

    @GetMapping("/approvals")
    public String approvals(
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            ) int page,
            Authentication authentication,
            Model model
    ) {
        model.addAttribute(
                "approvals",
                approvalHistoryService.getDecisions(
                        authentication.getName(),
                        LEVEL_1_CHECKER,
                        page
                )
        );

        return "dgm/approval-history";
    }

    /*
     * ============================================================
     * EMPLOYEE LIST
     * ============================================================
     */

    @GetMapping("/employees")
    public String employees(
            @RequestParam(
                    name = "query",
                    defaultValue = ""
            ) String query,
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            ) int page,
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

    /*
     * ============================================================
     * EMPLOYEE UPDATE REQUEST
     * ============================================================
     */

    @PostMapping("/employees/{id}/update-request")
    public String requestEmployeeUpdate(
            @PathVariable("id") Long id,
            @RequestParam("justification") String justification,
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
                    "Update request submitted successfully."
            );
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
        }

        return "redirect:/dgm/employees";
    }

    /*
     * ============================================================
     * SERIAL NUMBER HELPERS
     * ============================================================
     */

    /**
     * Adds the latest serial-number history record for one employee.
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

    /**
     * Adds the latest serial-number history record for each employee.
     */
    private void addLatestSerialNumbers(
            Model model,
            Iterable<Employee> employees
    ) {
        Map<Long, EmployeeSerialNumber> serialNumbersByEmployeeId =
                new LinkedHashMap<>();

        for (Employee employee : employees) {
            if (employee == null
                    || employee.getId() == null) {
                continue;
            }

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