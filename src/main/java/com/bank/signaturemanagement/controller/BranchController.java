package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.repository.BranchRepository;
import com.bank.signaturemanagement.repository.DesignationRepository;
import com.bank.signaturemanagement.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/branch")
public class BranchController {
    private final EmployeeService employeeService;
    private final DesignationRepository designationRepository;
    private final BranchRepository branchRepository;

    public BranchController(EmployeeService employeeService, DesignationRepository designationRepository,
                            BranchRepository branchRepository) {
        this.employeeService = employeeService;
        this.designationRepository = designationRepository;
        this.branchRepository = branchRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "") String query,
                            @RequestParam(required = false) Long designationId,
                            @RequestParam(required = false) Long branchId,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("query", query);
        model.addAttribute("designationId", designationId);
        model.addAttribute("branchId", branchId);
        model.addAttribute("designations", designationRepository.findByIsActiveTrueOrderByDesignationNameAsc());
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByBranchNameAsc());
        model.addAttribute("employees", employeeService.filter(query, null, designationId, branchId, page));
        return "branch/dashboard";
    }

    @GetMapping("/employees/{id}")
    public String employeeCard(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        return "branch/employee-card";
    }
}
