package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import com.bank.signaturemanagement.service.EmployeeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

@Controller
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;
    private final EmployeeMediaVersionRepository mediaVersionRepository;

    public EmployeeController(EmployeeService employeeService, EmployeeMediaVersionRepository mediaVersionRepository) { this.employeeService = employeeService;
        this.mediaVersionRepository = mediaVersionRepository;
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
    public String card(@PathVariable Long id, @RequestParam(required = false) String returnTo, Authentication authentication, HttpServletRequest request, Model model) {
        model.addAttribute("employee", employeeService.getEmployee(id));
        model.addAttribute("currentRole", roleName(authentication));
        model.addAttribute("media-version",mediaVersionRepository.findByEmployeeIdOrderByVersionNumberDesc(id));
        model.addAttribute("backUrl", resolveReturnTo(returnTo, request));
        return "employee/card";
    }

    private String safeReturnTo(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/") || value.startsWith("//") || value.contains("\\") || value.contains("\r") || value.contains("\n")) return "/employees";
        return value;
    }

    private String resolveReturnTo(String explicit, HttpServletRequest request) {
        if (explicit != null && !explicit.isBlank()) return safeReturnTo(explicit);
        String referer=request.getHeader("Referer");
        if(referer==null||referer.isBlank())return "/employees";
        try { URI uri=URI.create(referer);String path=uri.getRawPath();String query=uri.getRawQuery();return safeReturnTo(path+(query==null?"":"?"+query)); }
        catch(IllegalArgumentException exception){return "/employees";}
    }

    private String roleName(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
