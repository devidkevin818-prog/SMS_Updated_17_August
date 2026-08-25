package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
import com.bank.signaturemanagement.service.EmployeeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dgm")
public class DgmController {
    private final EmployeeRequestService requestService;
    private final ApprovalHistoryService approvalHistoryService;
    private final EmployeeService employeeService;


    public DgmController(EmployeeRequestService requestService, ApprovalHistoryService approvalHistoryService, EmployeeService employeeService) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
        this.employeeService = employeeService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.getPendingRequests(RequestStatus.PENDING_DGM, page));
        return "dgm/dashboard";
    }

    @GetMapping("/requests/{id}")
    public String review(@PathVariable Long id, Model model) {
        model.addAttribute("request", requestService.getRequest(id));
        model.addAttribute("approvalForm", new ApprovalForm());
        return "dgm/request-review";
    }

    @PostMapping("/requests/{id}/decision")
    public String decide(@PathVariable Long id, @RequestParam String action,
                         @ModelAttribute ApprovalForm approvalForm, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            requestService.dgmDecision(id, action, approvalForm.getRemark(), authentication.getName());
            redirectAttributes.addFlashAttribute("success", "DGM decision saved");
            return "redirect:/dgm/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/dgm/requests/" + id;
        }
    }

    @GetMapping("/approvals")
    public String approvals(@RequestParam(defaultValue = "0") int page,
                            Authentication authentication, Model model) {
        model.addAttribute("approvals", approvalHistoryService.getDecisions(authentication.getName(), "DGM", page));
        return "dgm/approval-history";
    }
    @GetMapping("/employees")
    public String employees(@RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        model.addAttribute("employees", requestService.searchEmployeesWithoutPendingRequest(query, page));
        model.addAttribute("query", query);

        return "dgm/employee-list";
    }
    @PostMapping("/employees/{id}/update-request")
    public String requestEmployeeUpdate(@PathVariable Long id,
                                        RedirectAttributes redirectAttributes) {


        try {
            employeeService.requestUpdate(id);
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


}
