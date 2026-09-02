package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/audit")
@org.springframework.security.access.prepost.PreAuthorize("@accessControl.has(authentication.name,'AUDIT_VIEW')")
public class RelatedAuditController {
    private final AuditService audit;

    public RelatedAuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "0") int page, Authentication auth, Model model) {
        model.addAttribute("logs", audit.relatedTo(auth.getName(), page));
        model.addAttribute("query", "");
        model.addAttribute("action", "");
        model.addAttribute("auditRole", "BRANCH");
        model.addAttribute("scopedAudit", true);
        return "admin/audit";
    }
}
