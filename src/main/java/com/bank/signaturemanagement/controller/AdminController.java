package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.dto.UserUpdateForm;
import com.bank.signaturemanagement.dto.AdminPasswordResetForm;
import com.bank.signaturemanagement.service.UserService;
import com.bank.signaturemanagement.service.UserApprovalService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final UserApprovalService userApprovalService;

    public AdminController(UserService userService, UserApprovalService userApprovalService) {
        this.userService = userService;
        this.userApprovalService = userApprovalService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "") String query,
                        @RequestParam(defaultValue = "") String role,
                        @RequestParam(defaultValue = "") String branch,
                        @RequestParam(defaultValue = "") String status,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        Boolean active = "active".equalsIgnoreCase(status) ? Boolean.TRUE
                : "inactive".equalsIgnoreCase(status) ? Boolean.FALSE : null;
        var users = userService.searchUsers(query, role, branch, active, page);
        model.addAttribute("users", users);
        model.addAttribute("roles", userService.getRoles());
        addCreatorViewData(model);
        model.addAttribute("branches", userService.getBranches());
        model.addAttribute("branchNames", userService.getBranchNamesById());
        model.addAttribute("duplicateEmployeeNumbers", userService.getDuplicateEmployeeNumbers());
        model.addAttribute("totalEmployees", userService.getTotalUserCount());
        model.addAttribute("activeEmployees", userService.getActiveUserCount());
        model.addAttribute("inactiveEmployees", userService.getInactiveUserCount());
        model.addAttribute("query", query);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedBranch", branch);
        model.addAttribute("selectedStatus", status);
        int firstPage = Math.max(0, users.getNumber() - 2);
        int lastPage = Math.min(users.getTotalPages() - 1, firstPage + 4);
        firstPage = Math.max(0, lastPage - 4);
        model.addAttribute("pageNumbers", users.getTotalPages() == 0
                ? java.util.List.of()
                : IntStream.rangeClosed(firstPage, lastPage).boxed().toList());
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("userForm")) model.addAttribute("userForm", new UserForm());
        model.addAttribute("branches", userService.getBranches());
        model.addAttribute("roles", userService.getRoles());
        addCreatorViewData(model);
        return "admin/create-user";
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute UserForm userForm, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (!result.hasErrors()) {
            try {
                boolean bootstrap=userApprovalService.propose(userForm,authentication.getName());
                redirectAttributes.addFlashAttribute("success", bootstrap ? "Bootstrap user created and activated" : "User request submitted for DGM approval");
                return "redirect:/admin/users";
            } catch (IllegalArgumentException exception) {
                result.reject("user", exception.getMessage());
            }
        }
        model.addAttribute("branches", userService.getBranches());
        model.addAttribute("roles", userService.getRoles());
        return "admin/create-user";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleActive(id);
        redirectAttributes.addFlashAttribute("success", "User status updated");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUser(id));
        model.addAttribute("userUpdateForm", userService.getUpdateForm(id));
        model.addAttribute("roles", userService.getRoles());
        model.addAttribute("branches", userService.getBranches());
        return "admin/edit-user";
    }

    @PostMapping("/users/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute UserUpdateForm userUpdateForm,
                         BindingResult result, Model model,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                userService.updateUser(id, userUpdateForm);
                redirectAttributes.addFlashAttribute("success", "User updated successfully");
                return "redirect:/admin/users";
            } catch (IllegalArgumentException exception) {
                result.reject("user", exception.getMessage());
            }
        }
        model.addAttribute("user", userService.getUser(id));
        model.addAttribute("roles", userService.getRoles());
        model.addAttribute("branches", userService.getBranches());
        return "admin/edit-user";
    }

    @GetMapping("/users/{id}/reset-password")
    public String resetPasswordForm(@PathVariable Long id, Model model,
                                    HttpServletResponse response) {
        preventSensitivePageCaching(response);
        try {
            var user = userService.getUser(id);
            AdminPasswordResetForm form = new AdminPasswordResetForm();
            form.setGeneratedPassword(userService.generateTemporaryPassword());
            model.addAttribute("user", user);
            model.addAttribute("branchName", userService.getBranchName(user.getBranchId()));
            model.addAttribute("adminPasswordResetForm", form);
            return "admin/reset-password";
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("errorTitle", "Employee not found");
            model.addAttribute("errorMessage",
                    "The employee account may have been removed or is no longer available.");
            return "admin/reset-password-error";
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @Valid @ModelAttribute AdminPasswordResetForm adminPasswordResetForm,
                                BindingResult result, Model model,
                                HttpServletResponse response) {
        preventSensitivePageCaching(response);
        if (!result.hasErrors()) {
            try {
                var user = userService.getUser(id);
                String password = userService.resetPasswordSecure(id, adminPasswordResetForm);
                model.addAttribute("user", user);
                model.addAttribute("temporaryPassword",
                        "generate".equals(adminPasswordResetForm.getResetMethod()) ? password : null);
                model.addAttribute("passwordChangeRequired", adminPasswordResetForm.isRequirePasswordChange());
                return "admin/reset-password-success";
            } catch (IllegalArgumentException exception) {
                result.reject("passwordReset", exception.getMessage());
            }
        }
        try {
            var user = userService.getUser(id);
            model.addAttribute("user", user);
            model.addAttribute("branchName", userService.getBranchName(user.getBranchId()));
            return "admin/reset-password";
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("errorTitle", "Employee not found");
            model.addAttribute("errorMessage",
                    "The employee account may have been removed or is no longer available.");
            return "admin/reset-password-error";
        }
    }

    private void preventSensitivePageCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
    }
    private void addCreatorViewData(Model model){model.addAttribute("creatorRole","ADMIN");model.addAttribute("creatorBackPath","/admin/users");model.addAttribute("userCreateAction","/admin/users");}
}
