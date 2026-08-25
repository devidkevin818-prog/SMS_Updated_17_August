package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.PasswordChangeForm;
import com.bank.signaturemanagement.service.FirstLoginService;
import jakarta.validation.Valid;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/account")
public class AccountController {
    private final FirstLoginService firstLoginService;

    public AccountController(FirstLoginService firstLoginService) { this.firstLoginService = firstLoginService; }

    @GetMapping("/change-password")
    public String form(Model model) {
        if (!model.containsAttribute("passwordChangeForm")) {
            model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        }
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String change(@Valid @ModelAttribute PasswordChangeForm passwordChangeForm,
                         BindingResult result, Authentication authentication,
                         HttpServletRequest request) {
        if (!result.hasErrors()) {
            try {
                firstLoginService.changePassword(authentication.getName(), passwordChangeForm);
                request.logout();
                return "redirect:/login?passwordChanged";
            } catch (IllegalArgumentException exception) {
                result.reject("password", exception.getMessage());
            } catch (ServletException exception) {
                result.reject("password", "Password changed, but automatic sign-out failed. Please sign out manually.");
            }
        }
        return "account/change-password";
    }
}
