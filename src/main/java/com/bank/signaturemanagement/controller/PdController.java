package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
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
    private final EmployeeMediaVersionRepository mediaVersionRepository;

    public PdController(EmployeeRequestService requestService,
                        EmployeeService employeeService,
                        EmployeeMediaVersionRepository mediaVersionRepository) {
        this.requestService = requestService;
        this.employeeService = employeeService;
        this.mediaVersionRepository = mediaVersionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "pd/dashboard";
    }

    @GetMapping("/employees/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("employeeRequestForm")) {
            model.addAttribute("employeeRequestForm", new EmployeeRequestForm());
        }
        return "pd/create-employee";
    }

    @PostMapping("/employees")
    public String create(@Valid @ModelAttribute EmployeeRequestForm employeeRequestForm,
                         BindingResult result,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {

        if (!result.hasErrors()) {
            try {
                requestService.createRequest(
                        employeeRequestForm,
                        authentication.getName()
                );

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Employee request submitted to DGM"
                );

                return "redirect:/pd/requests";

            } catch (IllegalArgumentException | IllegalStateException exception) {
                result.reject("request", exception.getMessage());
            }
        }

        return "pd/create-employee";
    }

    @GetMapping("/requests")
    public String requests(@RequestParam(defaultValue = "0") int page,
                           Authentication authentication,
                           Model model) {

        model.addAttribute(
                "requests",
                requestService.getRequestsForUser(
                        authentication.getName(),
                        page
                )
        );

        return "pd/request-list";
    }

    @GetMapping("/employees")
    public String employees(@RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {

        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));

        return "pd/employee-list";
    }

    @GetMapping("/approved-signatures")
    public String approvedSignatures(@RequestParam(defaultValue = "") String query,
                                     @RequestParam(defaultValue = "0") int page,
                                     Model model) {

        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));

        return "pd/approved-signatures";
    }

    @GetMapping("/approved-signatures/{id}")
    public String approvedSignatureVersions(@PathVariable Long id,
                                            Model model) {

        model.addAttribute(
                "employee",
                employeeService.getEmployee(id)
        );

        model.addAttribute(
                "versions",
                mediaVersionRepository.findByEmployeeIdOrderByVersionNumberDesc(id)
        );

        return "pd/approved-signature-versions";
    }

    /**
     * Normal employee edit page.
     *
     * If rejectedRequestId is present, this page was opened
     * from the Update button of a rejected request.
     */
    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(
            @PathVariable Long id,
            @RequestParam(required = false) Long rejectedRequestId,
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "employee",
                employeeService.getEmployee(id)
        );

        model.addAttribute(
                "employeeUpdateForm",
                employeeService.getUpdateForm(id)
        );

        if (rejectedRequestId != null) {

            // Validate that this rejected request belongs to
            // the logged-in PD user and is available for update.
            Long targetEmployeeId =
                    requestService.getTargetEmployeeIdForUpdate(
                            rejectedRequestId,
                            authentication.getName()
                    );

            if (!targetEmployeeId.equals(id)) {
                throw new IllegalArgumentException(
                        "Invalid employee update request"
                );
            }

            model.addAttribute(
                    "rejectedRequestId",
                    rejectedRequestId
            );
        }

        return "pd/edit-employee";
    }

    /**
     * Submit employee update.
     */
    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(
            @PathVariable Long id,
            @RequestParam(required = false) Long rejectedRequestId,
            @Valid @ModelAttribute EmployeeUpdateForm employeeUpdateForm,
            BindingResult result,
            Model model,
            Authentication authentication,
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

                // Create a new update request
                requestService.createUpdateRequest(
                        id,
                        employeeUpdateForm,
                        authentication.getName()
                );

                // If this update came from a rejected request,
                // disable the Update button on the old request.
                if (rejectedRequestId != null) {
                    requestService.markUpdateRequestCompleted(
                            rejectedRequestId
                    );
                }

                redirectAttributes.addFlashAttribute(
                        "success",
                        "Employee update submitted to DGM for approval"
                );

                return "redirect:/pd/requests";

            } catch (IllegalArgumentException | IllegalStateException exception) {

                System.out.println("========== UPDATE ERROR ==========");
                System.out.println("ERROR: " + exception.getMessage());

                exception.printStackTrace();

                result.reject(
                        "employee",
                        exception.getMessage()
                );
            }
        }

        model.addAttribute(
                "employee",
                employeeService.getEmployee(id)
        );

        // Preserve rejected-request information if validation fails.
        if (rejectedRequestId != null) {
            model.addAttribute(
                    "rejectedRequestId",
                    rejectedRequestId
            );
        }

        return "pd/edit-employee";
    }

    /**
     * Entry point from the Update button shown on a rejected request.
     */
    @GetMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        EmployeeRequest request = requestService.getRequest(id);
        request.setRemark("");

        if (!request.getRequestedBy().getUsername().equals(authentication.getName())) {
            throw new IllegalArgumentException("You are not authorized to update this request");
        }



        model.addAttribute("request", request);

        return "pd/update-request";
    }
    @PostMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            @Valid @ModelAttribute("request") EmployeeRequest updatedRequest,
            BindingResult result,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeRequest request = requestService.getRequest(id);

        // Keep the original request available when validation fails
        model.addAttribute("request", request);

        if (result.hasErrors()) {
            return "pd/update-request";
        }

        try {

            requestService.updateRequest(
                    id,
                    updatedRequest,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Rejected request updated and resubmitted to DGM"
            );

            return "redirect:/pd/requests";

        } catch (IllegalArgumentException | IllegalStateException exception) {

            result.reject(
                    "request",
                    exception.getMessage()
            );

            return "pd/update-request";
        }
    }
}