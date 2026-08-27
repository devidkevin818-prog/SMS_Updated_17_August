package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import com.bank.signaturemanagement.repository.EmployeeSignatureSerialNumberRepository;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.EmployeeSignatureSerialNumberService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;
    private final EmployeeSignatureSerialNumberService signatureSerialNumberService;

    public EmployeeController(EmployeeService employeeService, EmployeeMediaVersionRepository mediaVersionRepository, EmployeeSignatureSerialNumberService employeeSignatureSerialNumberService) { this.employeeService = employeeService;
        this.mediaVersionRepository = mediaVersionRepository;
        this.signatureSerialNumberService = employeeSignatureSerialNumberService;
    }

    @GetMapping
    public String directory(@RequestParam(defaultValue = "") String query,
                            @RequestParam(defaultValue = "0") int page,
                            Authentication authentication, Model model) {
        model.addAttribute("query", query);
        model.addAttribute("employees", employeeService.search(query, page));
        model.addAttribute("currentRole", roleName(authentication));
        return "employee/directory";
    }

    @GetMapping("/{id}")
    public String card(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        model.addAttribute("currentRole", roleName(authentication));
        model.addAttribute("media-version",mediaVersionRepository.findByEmployeeIdOrderByVersionNumberDesc(id));
        return "employee/card";
    }

    private String roleName(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
    @GetMapping("/signature-serial-numbers")
    public String signatureSerialNumbers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        model.addAttribute("query", query);

        model.addAttribute(
                "employees",
                signatureSerialNumberService.getEmployeesWithSerialNumbers(
                        query,
                        page
                )
        );
        model.addAttribute("currentRole", roleName(authentication));


        return "employee/signature-serial-numbers";
    }
}
