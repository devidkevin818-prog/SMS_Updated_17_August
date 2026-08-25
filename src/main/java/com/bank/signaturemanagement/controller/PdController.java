package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.service.*;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pd")
public class PdController {
    private final EmployeeRequestService requestService;
    private final EmployeeService employeeService;
    private final ApprovedSignaturePdfService pdfService;
    private final DesignationService designationService;
    private final DepartmentService departmentService;
    private final BranchService branchService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;


    public PdController(EmployeeRequestService requestService,
                        EmployeeService employeeService,
                        ApprovedSignaturePdfService pdfService,
                        EmployeeMediaVersionRepository mediaVersionRepository,
                        DesignationService designationService,
                        DepartmentService departmentService,
                        BranchService branchService) {

        this.requestService = requestService;
        this.employeeService = employeeService;
        this.pdfService = pdfService;
        this.mediaVersionRepository = mediaVersionRepository;
        this.designationService = designationService;
        this.departmentService = departmentService;
        this.branchService = branchService;
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

        model.addAttribute("designations", designationService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("branches", branchService.findAll());

        return "pd/create-employee";
    }

    @PostMapping("/employees")
    public String create(@Valid @ModelAttribute EmployeeRequestForm employeeRequestForm,
                         BindingResult result, Authentication authentication,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                requestService.createRequest(employeeRequestForm, authentication.getName());
                redirectAttributes.addFlashAttribute("success", "Employee request submitted to DGM");
                return "redirect:/pd/requests";
            } catch (IllegalArgumentException | IllegalStateException exception) {
                result.reject("request", exception.getMessage());
            }
        }
        model.addAttribute("designations", designationService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("branches", branchService.findAll());
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

        model.addAttribute("designations", designationService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("branches", branchService.findAll());
        return "pd/edit-employee";
    }

    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute EmployeeUpdateForm employeeUpdateForm,
                                 BindingResult result, Model model, Authentication authentication,
                                 RedirectAttributes redirectAttributes) {


<<<<<<< HEAD
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


=======
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
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

<<<<<<< HEAD
                System.out.println("========== UPDATE ERROR ==========");
                System.out.println("ERROR: " + exception.getMessage());

                exception.printStackTrace();

                result.reject("employee", exception.getMessage());
=======
                result.reject(
                        "employee",
                        exception.getMessage()
                );
>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
            }

        }

        model.addAttribute("employee", employeeService.getEmployee(id));
        return "pd/edit-employee";
    }
<<<<<<< HEAD
=======

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
        request.setEmployeeCode(
                com.bank.signaturemanagement.service.EmployeeNumberFormat.editablePart(request.getEmployeeCode())
        );

        if (!request.getRequestedBy().getUsername()
                .equals(authentication.getName())) {

            throw new IllegalArgumentException(
                    "You are not authorized to update this request"
            );
        }

        // The request ID and employee ID are different.
        Employee employee = request.getTargetEmployee();

        if (employee == null) {
            throw new IllegalStateException(
                    "This request is not linked to an existing employee"
            );
        }

        request.setRemark("");

        model.addAttribute("request", request);
        model.addAttribute("employee", employee);

        return "pd/update-request";
    }

    @PostMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            @Valid @ModelAttribute("request") EmployeeRequest updatedRequest,
            BindingResult result,
            @RequestParam(value = "foreignSignature", required = false)
            MultipartFile foreignSignature,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        EmployeeRequest existingRequest =
                requestService.getRequest(id);

        if (result.hasErrors()) {
            model.addAttribute("request", updatedRequest);

            // Preserve existing file paths
            if (updatedRequest.getPhotoPath() == null) {
                updatedRequest.setPhotoPath(
                        existingRequest.getPhotoPath()
                );
            }

            if (updatedRequest.getSignaturePath() == null) {
                updatedRequest.setSignaturePath(
                        existingRequest.getSignaturePath()
                );
            }

            if (updatedRequest.getForeignSignaturePath() == null) {
                updatedRequest.setForeignSignaturePath(
                        existingRequest.getForeignSignaturePath()
                );
            }

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

            existingRequest.setUpdatedAfterRejection(true);
            return "redirect:/pd/requests";

        } catch (IllegalArgumentException | IllegalStateException exception) {

            result.reject(
                    "request",
                    exception.getMessage()
            );

            model.addAttribute("request", updatedRequest);

            return "pd/update-request";
        }
    }

    @GetMapping("/request/{id}/edit")
    public String editEmployeeRequest(
            @PathVariable("id") Long id,
            Model model) {

        EmployeeRequest request = requestService.getRequest(id);

        model.addAttribute("request", request);

        return "pd/update-request";
    }

>>>>>>> 902a0ff191f065118ce7dffba299f0b0c67231a8
}
