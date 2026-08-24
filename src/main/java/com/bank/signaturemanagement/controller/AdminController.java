package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.dto.UserUpdateForm;
import com.bank.signaturemanagement.dto.AdminPasswordResetForm;
import com.bank.signaturemanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", userService.getUsers(page));
        model.addAttribute("branches", userService.getBranches());
        model.addAttribute("roles", userService.getRoles());
        if (!model.containsAttribute("userForm")) model.addAttribute("userForm", new UserForm());
        return "admin/users";
    }

    @PostMapping("/users")
    public String create(@Valid @ModelAttribute UserForm userForm, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes) {


        // For checking error
//        if (result.hasErrors()) {
//
//            result.getFieldErrors().forEach(error -> {
//                System.out.println(
//                        "FIELD = " + error.getField()
//                                + " | VALUE = " + error.getRejectedValue()
//                                + " | MESSAGE = " + error.getDefaultMessage()
//                );
//            });
//
//            System.out.println("===== USER FORM =====");
//            System.out.println("username = " + userForm.getUsername());
//            System.out.println("fullName = " + userForm.getFullName());
//            System.out.println("email = " + userForm.getEmail());
//            System.out.println("branch = " + userForm.getBranchId());
//            System.out.println("roleName = " + userForm.getRoleName());
//        }


        if (!result.hasErrors()) {
            try {
                userService.createUser(userForm);
                redirectAttributes.addFlashAttribute("success", "User created successfully");
                return "redirect:/admin/users";
            } catch (IllegalArgumentException exception) {
                result.reject("user", exception.getMessage());
            }
        }
        model.addAttribute("users", userService.getUsers(0));
        model.addAttribute("branches", userService.getBranches());
        model.addAttribute("roles", userService.getRoles());
        return "admin/users";
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
        if (!model.containsAttribute("adminPasswordResetForm")) {
            model.addAttribute("adminPasswordResetForm", new AdminPasswordResetForm());
        }
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
        model.addAttribute("adminPasswordResetForm", new AdminPasswordResetForm());
        return "admin/edit-user";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @Valid @ModelAttribute AdminPasswordResetForm adminPasswordResetForm,
                                BindingResult result, Model model,
                                RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                userService.resetPassword(id, adminPasswordResetForm);
                redirectAttributes.addFlashAttribute("success",
                        "Password reset successfully. The user must change it at the next login.");
                return "redirect:/admin/users/{id}/edit";
            } catch (IllegalArgumentException exception) {
                result.reject("passwordReset", exception.getMessage());
            }
        }
        model.addAttribute("user", userService.getUser(id));
        model.addAttribute("userUpdateForm", userService.getUpdateForm(id));
        model.addAttribute("roles", userService.getRoles());
        model.addAttribute("branches", userService.getBranches());
        return "admin/edit-user";
    }
}
