package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.ReferenceDataForm;
import com.bank.signaturemanagement.service.ReferenceDataAdminService;
import com.bank.signaturemanagement.service.SystemSettingService;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/admin/reference-data", "/pd/reference-data"})
@org.springframework.security.access.prepost.PreAuthorize("@accessControl.has(authentication.name,'CONFIG_MANAGE')")
public class ReferenceDataController {
    private final ReferenceDataAdminService service;
    private final SystemSettingService settings;

    public ReferenceDataController(ReferenceDataAdminService service, SystemSettingService settings) {
        this.service = service;
        this.settings = settings;
    }

    @GetMapping
    public String index(Model model) {
        populate(model);
        if (!model.containsAttribute("departmentForm")) model.addAttribute("departmentForm", new ReferenceDataForm());
        if (!model.containsAttribute("designationForm")) model.addAttribute("designationForm", new ReferenceDataForm());
        if (!model.containsAttribute("branchForm")) model.addAttribute("branchForm", new ReferenceDataForm());
        if (!model.containsAttribute("statusForm")) model.addAttribute("statusForm", new ReferenceDataForm());
        return "reference-data/index";
    }

    @PostMapping("/departments")
    public String createDepartment(@Valid @ModelAttribute("departmentForm") ReferenceDataForm form,
                                   BindingResult result, Model model,
                                   RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        if (!result.hasErrors()) {
            try {
                service.createDepartment(form);
                redirect.addFlashAttribute("success", "Department added");
                return redirectRoot(request);
            } catch (IllegalArgumentException exception) {
                result.reject("department", exception.getMessage());
            }
        }
        model.addAttribute("designationForm", new ReferenceDataForm());
        populate(model);
        return "reference-data/index";
    }

    @PostMapping("/departments/{id}")
    public String updateDepartment(@PathVariable Long id, @Valid ReferenceDataForm form,
                                   BindingResult result, RedirectAttributes redirect,
                                   jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid department name");
        else try {
            service.updateDepartment(id, form);
            redirect.addFlashAttribute("success", "Department updated");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/departments/{id}/toggle")
    public String toggleDepartment(@PathVariable Long id, RedirectAttributes redirect,
                                   jakarta.servlet.http.HttpServletRequest request) {
        service.toggleDepartment(id);
        redirect.addFlashAttribute("success", "Department availability updated");
        return redirectRoot(request);
    }

    @PostMapping("/designations")
    public String createDesignation(@Valid @ModelAttribute("designationForm") ReferenceDataForm form,
                                    BindingResult result, Model model,
                                    RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        if (!result.hasErrors()) {
            try {
                service.createDesignation(form);
                redirect.addFlashAttribute("success", "Designation added");
                return redirectRoot(request);
            } catch (IllegalArgumentException exception) {
                result.reject("designation", exception.getMessage());
            }
        }
        model.addAttribute("departmentForm", new ReferenceDataForm());
        populate(model);
        return "reference-data/index";
    }

    @PostMapping("/designations/{id}")
    public String updateDesignation(@PathVariable Long id, @Valid ReferenceDataForm form,
                                    BindingResult result, RedirectAttributes redirect,
                                    jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid designation name");
        else try {
            service.updateDesignation(id, form);
            redirect.addFlashAttribute("success", "Designation updated");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/designations/{id}/toggle")
    public String toggleDesignation(@PathVariable Long id, RedirectAttributes redirect,
                                    jakarta.servlet.http.HttpServletRequest request) {
        service.toggleDesignation(id);
        redirect.addFlashAttribute("success", "Designation availability updated");
        return redirectRoot(request);
    }

    @PostMapping("/branches")
    public String createBranch(@Valid ReferenceDataForm form, BindingResult result, RedirectAttributes redirect,
                               jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid branch name");
        else try {
            service.createBranch(form);
            redirect.addFlashAttribute("success", "Branch added");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/branches/{id}")
    public String updateBranch(@PathVariable Long id, @Valid ReferenceDataForm form, BindingResult result,
                               RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid branch name");
        else try {
            service.updateBranch(id, form);
            redirect.addFlashAttribute("success", "Branch updated");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/branches/{id}/toggle")
    public String toggleBranch(@PathVariable Long id, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        service.toggleBranch(id);
        redirect.addFlashAttribute("success", "Branch availability updated");
        return redirectRoot(request);
    }

    @PostMapping("/statuses")
    public String createStatus(@Valid ReferenceDataForm form, BindingResult result, RedirectAttributes redirect,
                               jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid employee status");
        else try {
            service.createStatus(form);
            redirect.addFlashAttribute("success", "Employee status added");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/statuses/{id}")
    public String updateStatus(@PathVariable Long id, @Valid ReferenceDataForm form, BindingResult result,
                               RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        if (result.hasErrors()) redirect.addFlashAttribute("error", "Enter a valid employee status");
        else try {
            service.updateStatus(id, form);
            redirect.addFlashAttribute("success", "Employee status updated");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    @PostMapping("/statuses/{id}/toggle")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        service.toggleStatus(id);
        redirect.addFlashAttribute("success", "Employee status availability updated");
        return redirectRoot(request);
    }

    @PostMapping("/settings/employee-id-regex")
    public String updateEmployeeIdRegex(@RequestParam String regex, Authentication authentication,
                                        RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            settings.updateEmployeeIdRegex(regex, authentication.getName());
            redirect.addFlashAttribute("success", "Employee ID format updated");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return redirectRoot(request);
    }

    private void populate(Model model) {
        model.addAttribute("departments", service.departments());
        model.addAttribute("designations", service.designations());
        model.addAttribute("branches", service.branches());
        model.addAttribute("employeeStatuses", service.statuses());
        model.addAttribute("employeeIdRegex", settings.employeeIdRegex());
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        model.addAttribute("referenceBasePath", admin
                ? "/admin/reference-data" : "/pd/reference-data");
        model.addAttribute("referenceRole", admin ? "ADMIN" : "PD");
    }

    private String redirectRoot(jakarta.servlet.http.HttpServletRequest request) {
        return "redirect:" + (request.getRequestURI().startsWith("/pd/")
                ? "/pd/reference-data" : "/admin/reference-data");
    }
}
