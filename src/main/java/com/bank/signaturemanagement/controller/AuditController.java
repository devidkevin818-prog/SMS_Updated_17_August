package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@org.springframework.security.access.prepost.PreAuthorize("@accessControl.has(authentication.name,'AUDIT_VIEW')")
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String query,
                        @RequestParam(defaultValue = "") String action,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("logs", audit.search(query, action, page));
        model.addAttribute("query", query);
        model.addAttribute("action", action);
        model.addAttribute("auditRole", "ADMIN");
        model.addAttribute("scopedAudit", false);
        return "admin/audit";
    }
}
