package com.bank.signaturemanagement.security;

import com.bank.signaturemanagement.service.UserService;
import com.bank.signaturemanagement.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final AuditService auditService;

    public RoleLoginSuccessHandler(
            UserService userService,
            AuditService auditService
    ) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        userService.recordSuccessfulLogin(
                authentication.getName()
        );

        auditService.record(
                authentication.getName(),
                "LOGIN",
                "SESSION",
                request.getSession().getId(),
                request.getRemoteAddr(),
                "SUCCESS",
                null,
                null,
                null
        );

        String role = authentication
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        String target = switch (role) {
            case "ROLE_ADMIN" -> "/admin/dashboard";
            case "ROLE_MAKER" -> "/pd/dashboard";
            case "ROLE_LEVEL_1_CHECKER" -> "/dgm/dashboard";
            case "ROLE_LEVEL_2_CHECKER" -> "/gm/dashboard";
            case "ROLE_BRANCH" -> "/branch/dashboard";
            case "ROLE_AUDIT" -> "/audit/dashboard";
            default -> "/login?error";
        };

        response.sendRedirect(
                request.getContextPath() + target
        );
    }
}