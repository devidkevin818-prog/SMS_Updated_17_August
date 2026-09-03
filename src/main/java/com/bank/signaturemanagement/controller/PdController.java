package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.dto.EmployeeRequestForm;
import com.bank.signaturemanagement.dto.EmployeeUpdateForm;
import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeRequest;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import com.bank.signaturemanagement.service.ApprovedSignaturePdfService;
import com.bank.signaturemanagement.service.BranchService;
import com.bank.signaturemanagement.service.DepartmentService;
import com.bank.signaturemanagement.service.DesignationService;
import com.bank.signaturemanagement.service.EmployeeNumberFormat;
import com.bank.signaturemanagement.service.EmployeeRequestService;
import com.bank.signaturemanagement.service.EmployeeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.bank.signaturemanagement.repository.EmployeeStatusRepository;
import com.bank.signaturemanagement.service.UserService;
import com.bank.signaturemanagement.service.UserApprovalService;
import com.bank.signaturemanagement.dto.UserForm;
import com.bank.signaturemanagement.service.EmployeeChangeProposalService;


@Controller
@RequestMapping("/pd")
public class PdController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.bank.signaturemanagement.repository.EmployeeStatusRepository employeeStatusRepository;
    private final EmployeeRequestService requestService;
    private final EmployeeService employeeService;
    private final ApprovedSignaturePdfService pdfService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;
    private final DesignationService designationService;
    private final DepartmentService departmentService;
    private final BranchService branchService;
    private final UserService userService;
    private final UserApprovalService userApprovalService;
    private final EmployeeChangeProposalService changeProposalService;

    public PdController(
            EmployeeRequestService requestService,
            EmployeeService employeeService,
            ApprovedSignaturePdfService pdfService,
            EmployeeMediaVersionRepository mediaVersionRepository,
            DesignationService designationService,
            DepartmentService departmentService,
            BranchService branchService, UserService userService, UserApprovalService userApprovalService,
            EmployeeChangeProposalService changeProposalService) {
        this.requestService = requestService;
        this.employeeService = employeeService;
        this.pdfService = pdfService;
        this.mediaVersionRepository = mediaVersionRepository;
        this.designationService = designationService;
        this.departmentService = departmentService;
        this.branchService = branchService;
        this.userService = userService;
        this.userApprovalService = userApprovalService;
        this.changeProposalService = changeProposalService;
    }

    @GetMapping("/users/new")
    public String createUserForm(Model model){model.addAttribute("userForm",new UserForm());addUserReferenceData(model);return "admin/create-user";}
    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute UserForm form,BindingResult result,Authentication authentication,Model model,RedirectAttributes redirect){
        if(!result.hasErrors())try{userApprovalService.propose(form,authentication.getName());redirect.addFlashAttribute("success","User request submitted for DGM approval");return "redirect:/pd/dashboard";}catch(IllegalArgumentException e){result.reject("user",e.getMessage());}
        addUserReferenceData(model);return "admin/create-user";
    }
    private void addUserReferenceData(Model model){model.addAttribute("branches",userService.getBranches());model.addAttribute("roles",userService.getRoles().stream().filter(r->!"ADMIN".equals(r.getName())).toList());model.addAttribute("creatorRole","PD");model.addAttribute("creatorBackPath","/pd/dashboard");model.addAttribute("userCreateAction","/pd/users");}

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        var changeProposals = changeProposalService.pendingPd(authentication.getName());
        model.addAttribute("changeProposals", changeProposals);
        model.addAttribute("changeProposalCount", changeProposals.size());
        model.addAttribute("myRequestCount",
                requestService.getRequestsForUser(authentication.getName(), 0).getTotalElements());
        return "pd/dashboard";
    }

    @PostMapping("/employees/{id}/toggle-lock")
    public String toggleLock(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        changeProposalService.toggleLock(id, authentication.getName());
        redirect.addFlashAttribute("success", "Employee edit lock updated");
        return "redirect:/pd/employees";
    }

    @PostMapping("/change-proposals/{id}/accept")
    public String acceptProposal(@PathVariable Long id, Authentication authentication) {
        var proposal=changeProposalService.acceptForEditing(id, authentication.getName());
        return "redirect:/pd/employees/"+proposal.getEmployee().getId()+"/edit?proposalId="+id;
    }

    @GetMapping("/employees/new")
    public String createForm(Model model) {

        if (!model.containsAttribute("employeeRequestForm")) {

            EmployeeRequestForm form = new EmployeeRequestForm();
            employeeStatusRepository.findByActiveTrueOrderByDisplayOrderAscStatusNameAsc().stream().findFirst()
                    .ifPresent(status -> form.setStatusId(status.getStatusId()));
            model.addAttribute(
                    "employeeRequestForm",
                    form
            );
        }
        addReferenceData(model);
        return "pd/create-employee";
    }


    @PostMapping("/employees")
    public String create(
            @Valid @ModelAttribute EmployeeRequestForm employeeRequestForm,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (!result.hasErrors()) {
            try {
                requestService.createRequest(employeeRequestForm, authentication.getName());
                redirectAttributes.addFlashAttribute("success", "Employee request submitted to DGM");
                return "redirect:/pd/requests";
            } catch (IllegalArgumentException | IllegalStateException exception) {

                result.reject(
                        "request",
                        exception.getMessage()
                );
            }
        }
        addReferenceData(model);
        return "pd/create-employee";
    }


    @GetMapping("/requests")
    public String requests(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        model.addAttribute("requests", requestService.getRequestsForUser(authentication.getName(), page));
        return "pd/request-list";
    }

    @GetMapping("/employees")
    public String employees(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        return "pd/employee-list";
    }

    @GetMapping("/approved-signatures")
    public String approvedSignatures(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        return "pd/approved-signatures";
    }

    @GetMapping("/approved-signatures/pdf")
    public void downloadApprovedPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=approved-signatures.pdf");
        pdfService.generateApprovedPdf(response.getOutputStream());
    }

    @GetMapping("/approved-signatures/{id}")
    public String approvedSignatureVersions(@PathVariable Long id, Model model) {
        var employee=employeeService.getEmployee(id);
        model.addAttribute("employee", employee);
        model.addAttribute("versions", mediaVersionRepository.findByEmployeeIdOrderByVersionNumberDesc(id));
        return "pd/approved-signature-versions";
    }

    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(
            @PathVariable Long id,
            @RequestParam(required = false) Long rejectedRequestId,
            @RequestParam(required = false) Long proposalId,
            Authentication authentication,
            Model model) {
        var employee = employeeService.getEmployee(id);
        if (proposalId == null) {
            throw new IllegalStateException("DGM or GM must initiate this employee update first");
        }
        changeProposalService.requireEditing(proposalId, id, authentication.getName());
        model.addAttribute("employee", employee);
        model.addAttribute("employeeUpdateForm", employeeService.getUpdateForm(id));
        model.addAttribute("proposalId", proposalId);
        addReferenceData(model);

        if (rejectedRequestId != null) {
            Long targetEmployeeId = requestService.getTargetEmployeeIdForUpdate(
                    rejectedRequestId, authentication.getName());
            if (!targetEmployeeId.equals(id)) {
                throw new IllegalArgumentException("Invalid employee update request");
            }
            model.addAttribute("rejectedRequestId", rejectedRequestId);
        }
        return "pd/edit-employee";
    }

    @PostMapping("/employees/{id}/edit")
    public String updateEmployee(
            @PathVariable Long id,
            @RequestParam(required = false) Long rejectedRequestId,
            @RequestParam(required = false) Long proposalId,
            @Valid @ModelAttribute EmployeeUpdateForm employeeUpdateForm,
            BindingResult result,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                var proposal=proposalId==null?null:changeProposalService.requireEditing(proposalId,id,authentication.getName());
                if(proposal==null) throw new IllegalStateException("DGM or GM must initiate this employee update first");
                requestService.createUpdateRequest(id, employeeUpdateForm, authentication.getName(),proposal);
                if (proposalId != null) changeProposalService.markSubmitted(proposalId, authentication.getName());
                employeeService.updateRequestStatus(id, false);
                if (rejectedRequestId != null) {
                    requestService.markUpdateRequestCompleted(rejectedRequestId);
                }
                redirectAttributes.addFlashAttribute(
                        "success", "Employee update submitted to DGM for approval");
                return "redirect:/pd/requests";
            } catch (IllegalArgumentException | IllegalStateException exception) {
                result.reject("employee", exception.getMessage());
            }
        }

        model.addAttribute("employee", employeeService.getEmployee(id));
        if (rejectedRequestId != null) {
            model.addAttribute("rejectedRequestId", rejectedRequestId);
        }
        addReferenceData(model);
        return "pd/edit-employee";
    }

    @GetMapping("/requests/{id}/update")
    public String updateRejectedRequest(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        EmployeeRequest request = requestService.getRequest(id);
        requireOriginalRequester(request, authentication.getName());

        Employee employee = request.getTargetEmployee();
        if (employee == null) {
            throw new IllegalStateException("This request is not linked to an existing employee");
        }

        request.setEmployeeCode(EmployeeNumberFormat.editablePart(request.getEmployeeCode()));
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
            @RequestParam(value = "foreignSignature", required = false) MultipartFile foreignSignature,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        EmployeeRequest existingRequest = requestService.getRequest(id);
        preserveFilePaths(updatedRequest, existingRequest);

        if (result.hasErrors()) {
            model.addAttribute("request", updatedRequest);
            model.addAttribute("employee", existingRequest.getTargetEmployee());
            return "pd/update-request";
        }

        try {
            requestService.updateRequest(
                    id, updatedRequest, foreignSignature, authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "success", "Rejected request updated and resubmitted to DGM");
            return "redirect:/pd/requests";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            result.reject("request", exception.getMessage());
            model.addAttribute("request", updatedRequest);
            model.addAttribute("employee", existingRequest.getTargetEmployee());
            return "pd/update-request";
        }
    }

    @GetMapping("/request/{id}/edit")
    public String editEmployeeRequest(@PathVariable("id") Long id, Model model) {
        EmployeeRequest request = requestService.getRequest(id);
        model.addAttribute("request", request);
        model.addAttribute("employee", request.getTargetEmployee());
        return "pd/update-request";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("designations", designationService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("branches", branchService.findAll());
        model.addAttribute("employeeStatuses", employeeStatusRepository.findByActiveTrueOrderByDisplayOrderAscStatusNameAsc());
    }

    private void requireOriginalRequester(EmployeeRequest request, String username) {
        if (!request.getRequestedBy().getUsername().equals(username)) {
            throw new IllegalArgumentException("You are not authorized to update this request");
        }
    }

    private void preserveFilePaths(EmployeeRequest updated, EmployeeRequest existing) {
        if (updated.getPhotoPath() == null) {
            updated.setPhotoPath(existing.getPhotoPath());
        }
        if (updated.getSignaturePath() == null) {
            updated.setSignaturePath(existing.getSignaturePath());
        }
        if (updated.getForeignSignaturePath() == null) {
            updated.setForeignSignaturePath(existing.getForeignSignaturePath());
        }
    }
}
