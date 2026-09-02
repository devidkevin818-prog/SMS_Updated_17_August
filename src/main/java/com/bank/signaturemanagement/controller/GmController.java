package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
import com.bank.signaturemanagement.service.UserApprovalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gm")
public class GmController {
    private final EmployeeRequestService requestService;
    private final ApprovalHistoryService approvalHistoryService;
    private final UserApprovalService userApprovalService;
    private final com.bank.signaturemanagement.service.EmployeeChangeProposalService changeProposalService;
    private final com.bank.signaturemanagement.service.BatchImportService batchImportService;
    private final com.bank.signaturemanagement.service.SignatureWorkflowService signatureWorkflowService;
    private final com.bank.signaturemanagement.service.EmployeeMediaRequestService mediaRequestService;
    public GmController(EmployeeRequestService requestService, ApprovalHistoryService approvalHistoryService, UserApprovalService userApprovalService, com.bank.signaturemanagement.service.EmployeeChangeProposalService changeProposalService, com.bank.signaturemanagement.service.BatchImportService batchImportService, com.bank.signaturemanagement.service.SignatureWorkflowService signatureWorkflowService,com.bank.signaturemanagement.service.EmployeeMediaRequestService mediaRequestService) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
        this.userApprovalService = userApprovalService;
        this.changeProposalService = changeProposalService;
        this.batchImportService = batchImportService;
        this.signatureWorkflowService = signatureWorkflowService;
        this.mediaRequestService = mediaRequestService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.getPendingRequests(RequestStatus.PENDING_GM, page));
        model.addAttribute("userRequests", userApprovalService.pending("GM"));
        model.addAttribute("batchRequests", batchImportService.pending("GM"));
        model.addAttribute("signatureRequests", signatureWorkflowService.pending("GM"));
        model.addAttribute("mediaRequests", mediaRequestService.pending("GM"));
        return "gm/dashboard";
    }
    @PostMapping("/batch-requests/{id}/decision")
    public String batchDecision(@PathVariable Long id,@RequestParam String action,@RequestParam(required=false)String comment,Authentication authentication,RedirectAttributes redirect){try{batchImportService.decide(id,"GM",action,comment,authentication.getName());redirect.addFlashAttribute("success","Batch decision saved");}catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:/gm/dashboard";}
    @GetMapping("/batch-requests/{id}")
    public String batchView(@PathVariable Long id,Model model){model.addAttribute("batch",batchImportService.get(id));model.addAttribute("items",batchImportService.itemViews(id));model.addAttribute("batchBase","/gm/dashboard");model.addAttribute("pageRole","GM");model.addAttribute("batchReadOnly",true);return "batches/detail";}
    @PostMapping("/employees/{id}/update-request")
    public String requestEmployeeUpdate(@PathVariable Long id, @RequestParam String justification,
                                        Authentication authentication, RedirectAttributes redirect) {
        try { changeProposalService.submit(id, justification, authentication.getName()); redirect.addFlashAttribute("success", "Proposal sent directly to PD"); }
        catch (IllegalArgumentException e) { redirect.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/employees";
    }
    @PostMapping("/user-requests/{id}/decision")
    public String userDecision(@PathVariable Long id,@RequestParam String action,@RequestParam(required=false) String comment,Authentication authentication,RedirectAttributes redirect){
        try{userApprovalService.decide(id,"GM",action,comment,authentication.getName());redirect.addFlashAttribute("success","User request decision saved");}
        catch(IllegalArgumentException e){redirect.addFlashAttribute("error",e.getMessage());} return "redirect:/gm/dashboard";
    }

    @GetMapping("/requests/{id}")
    public String review(@PathVariable Long id, Model model) {
        model.addAttribute("request", requestService.getRequest(id));
        model.addAttribute("approvalForm", new ApprovalForm());
        return "gm/request-review";
    }

    @PostMapping("/requests/{id}/decision")
    public String decide(@PathVariable Long id, @RequestParam String action,
                         @ModelAttribute ApprovalForm approvalForm, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            requestService.gmDecision(id, action, approvalForm.getRemark(), authentication.getName());
            redirectAttributes.addFlashAttribute("success", "GM decision saved");
            return "redirect:/gm/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/gm/requests/" + id;
        }
    }

    @GetMapping("/approvals")
    public String approvals(@RequestParam(defaultValue = "0") int page,
                            Authentication authentication, Model model) {
        model.addAttribute("approvals", approvalHistoryService.getDecisions(authentication.getName(), "GM", page));
        return "gm/approval-history";
    }
}
