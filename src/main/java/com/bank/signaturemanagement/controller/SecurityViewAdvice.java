package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.AccessControlService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SecurityViewAdvice {
    private final AccessControlService access;

    public SecurityViewAdvice(AccessControlService access) {
        this.access = access;
    }

    @ModelAttribute
    public void addVisibilityFlags(Model model, Authentication authentication) {
        boolean local = false;
        boolean foreign = false;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                local = access.canViewSignature(authentication.getName(), "LOCAL");
                foreign = access.canViewSignature(authentication.getName(), "FOREIGN");
            } catch (RuntimeException ignored) {
                // A missing/deactivated account receives no signature visibility.
            }
        }
        model.addAttribute("canViewLocal", local);
        model.addAttribute("canViewForeign", foreign);
    }
}
