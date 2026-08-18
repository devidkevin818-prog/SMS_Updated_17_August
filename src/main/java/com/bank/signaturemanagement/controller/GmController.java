package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ApprovalForm;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.ApprovalHistoryService;
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
    public GmController(EmployeeRequestService requestService, ApprovalHistoryService approvalHistoryService) {
        this.requestService = requestService;
        this.approvalHistoryService = approvalHistoryService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("requests", requestService.getPendingRequests(RequestStatus.PENDING_GM, page));
        return "gm/dashboard";
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
