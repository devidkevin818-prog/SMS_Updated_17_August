package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.ApprovedSignaturePdfService;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pd")
public class PdController {
    private final EmployeeRequestService requestService;
    private final EmployeeService employeeService;
    private final ApprovedSignaturePdfService pdfService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;

    public PdController(EmployeeRequestService requestService,
                        EmployeeService employeeService,
                        ApprovedSignaturePdfService pdfService,
                        EmployeeMediaVersionRepository mediaVersionRepository) {

        this.requestService = requestService;
        this.employeeService = employeeService;
        this.pdfService = pdfService;
        this.mediaVersionRepository = mediaVersionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "pd/dashboard";
    }

    @GetMapping("/employees/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("employeeRequestForm"))
            model.addAttribute("employeeRequestForm", new EmployeeRequestForm());
        return "pd/create-employee";
    }

    @PostMapping("/employees")
    public String create(@Valid @ModelAttribute EmployeeRequestForm employeeRequestForm,
                         BindingResult result, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                requestService.createRequest(employeeRequestForm, authentication.getName());
                redirectAttributes.addFlashAttribute("success", "Employee request submitted to DGM");
                return "redirect:/pd/requests";
            } catch (IllegalArgumentException | IllegalStateException exception) {
                result.reject("request", exception.getMessage());
            }
        }
        return "pd/create-employee";
    }

    @GetMapping("/requests")
    public String requests(@RequestParam(defaultValue = "0") int page,
                           Authentication authentication, Model model) {
        model.addAttribute("requests", requestService.getRequestsForUser(authentication.getName(), page));
        return "pd/request-list";
    }

    @GetMapping("/employees")
    public String employees(@RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        return "pd/employee-list";
    }

    @GetMapping("/approved-signatures")
    public String approvedSignatures(@RequestParam(defaultValue = "") String query,
                                     @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        return "pd/approved-signatures";
    }

    @GetMapping("/approved-signatures/pdf")
    public void downloadApprovedPdf(
            HttpServletResponse response
    ) throws Exception {


        response.setContentType("application/pdf");

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=approved-signatures.pdf"
        );


        pdfService.generateApprovedPdf(
                response.getOutputStream()
        );
    }

    @GetMapping("/approved-signatures/{id}")
    public String approvedSignatureVersions(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        model.addAttribute("versions", mediaVersionRepository.findByEmployeeIdOrderByVersionNumberDesc(id));
        return "pd/approved-signature-versions";
    }

    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        model.addAttribute("employeeUpdateForm", employeeService.getUpdateForm(id));
        return "pd/edit-employee";
    }

    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute EmployeeUpdateForm employeeUpdateForm,
                                 BindingResult result, Model model, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {


        if (result.hasErrors()) {
            System.out.println("========== VALIDATION ERRORS ==========");
            result.getFieldErrors().forEach(error ->
                    System.out.println(
                            "FIELD: " + error.getField()
                                    + " | VALUE: [" + error.getRejectedValue() + "]"
                                    + " | MESSAGE: " + error.getDefaultMessage()
                    )
            );
        }


        if (!result.hasErrors()) {
            try {
                requestService.createUpdateRequest(
                        id,
                        employeeUpdateForm,
                        authentication.getName()
                );

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Employee update submitted to DGM for approval"
                );

                return "redirect:/pd/requests";

            } catch (IllegalArgumentException | IllegalStateException exception) {

                System.out.println("========== UPDATE ERROR ==========");
                System.out.println("ERROR: " + exception.getMessage());

                exception.printStackTrace();

                result.reject("employee", exception.getMessage());
            }

        }

        model.addAttribute("employee", employeeService.getEmployee(id));
        return "pd/edit-employee";
    }
}
