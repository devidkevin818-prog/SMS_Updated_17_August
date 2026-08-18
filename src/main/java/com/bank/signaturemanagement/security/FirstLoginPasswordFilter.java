package com.bank.signaturemanagement.security;

import com.bank.signaturemanagement.service.FirstLoginService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirstLoginPasswordFilter extends OncePerRequestFilter {
    private final FirstLoginService firstLoginService;

    public FirstLoginPasswordFilter(FirstLoginService firstLoginService) {
        this.firstLoginService = firstLoginService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        boolean allowedPath = path.equals("/account/change-password") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/");
        if (!allowedPath && authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                && firstLoginService.mustChangePassword(authentication.getName())) {
            response.sendRedirect(contextPath + "/account/change-password");
            return;
        }
        chain.doFilter(request, response);
    }
}
