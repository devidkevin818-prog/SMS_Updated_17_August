package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/branch")
public class BranchController {
    private final EmployeeService employeeService;
    public BranchController(EmployeeService employeeService) { this.employeeService = employeeService; }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        return "branch/dashboard";
    }

    @GetMapping("/employees/{id}")
    public String employeeCard(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        return "branch/employee-card";
    }
}
