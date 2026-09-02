package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.UserApprovalService;
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
    private final UserApprovalService userApprovalService;
    private final com.bank.signaturemanagement.service.EmployeeChangeProposalService changeProposalService;
    private final com.bank.signaturemanagement.service.BatchImportService batchImportService;
    private final com.bank.signaturemanagement.service.SignatureWorkflowService signatureWorkflowService;
    private final com.bank.signaturemanagement.service.EmployeeMediaRequestService mediaRequestService;


    public DgmController(EmployeeRequestService requestService, ApprovalHistoryService approvalHistoryService, EmployeeService employeeService, UserApprovalService userApprovalService, com.bank.signaturemanagement.service.EmployeeChangeProposalService changeProposalService, com.bank.signaturemanagement.service.BatchImportService batchImportService, com.bank.signaturemanagement.service.SignatureWorkflowService signatureWorkflowService,com.bank.signaturemanagement.service.EmployeeMediaRequestService mediaRequestService) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
        this.employeeService = employeeService;
        this.userApprovalService = userApprovalService;
        this.changeProposalService = changeProposalService;
        this.batchImportService = batchImportService;
        this.signatureWorkflowService = signatureWorkflowService;
        this.mediaRequestService = mediaRequestService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.getPendingRequests(RequestStatus.PENDING_DGM, page));
        model.addAttribute("userRequests", userApprovalService.pending("DGM"));
        model.addAttribute("batchRequests", batchImportService.pending("DGM"));
        model.addAttribute("signatureRequests", signatureWorkflowService.pending("DGM"));
        model.addAttribute("mediaRequests", mediaRequestService.pending("DGM"));
        return "dgm/dashboard";
    }
    @PostMapping("/batch-requests/{id}/decision")
    public String batchDecision(@PathVariable Long id,@RequestParam String action,@RequestParam(required=false)String comment,Authentication authentication,RedirectAttributes redirect){try{batchImportService.decide(id,"DGM",action,comment,authentication.getName());redirect.addFlashAttribute("success","Batch decision saved");}catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:/dgm/dashboard";}
    @GetMapping("/batch-requests/{id}")
    public String batchView(@PathVariable Long id,Model model){model.addAttribute("batch",batchImportService.get(id));model.addAttribute("items",batchImportService.itemViews(id));model.addAttribute("batchBase","/dgm/dashboard");model.addAttribute("pageRole","DGM");model.addAttribute("batchReadOnly",true);return "batches/detail";}
    @PostMapping("/user-requests/{id}/decision")
    public String userDecision(@PathVariable Long id,@RequestParam String action,@RequestParam(required=false) String comment,Authentication authentication,RedirectAttributes redirect){
        try{userApprovalService.decide(id,"DGM",action,comment,authentication.getName());redirect.addFlashAttribute("success","User request decision saved");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());} return "redirect:/dgm/dashboard";
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
                                        @RequestParam String justification,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {


        try {
            changeProposalService.submit(id, justification, authentication.getName());
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
