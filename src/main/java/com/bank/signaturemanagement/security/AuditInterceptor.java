package com.bank.signaturemanagement.security;

import com.bank.signaturemanagement.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditInterceptor implements HandlerInterceptor {
    private final AuditService audit;
    public AuditInterceptor(AuditService audit) { this.audit = audit; }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getRequestURI().startsWith("/css/") || request.getRequestURI().startsWith("/js/")) return;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        String action = switch (request.getMethod()) {
            case "GET" -> "VIEW"; case "POST" -> "CHANGE"; default -> request.getMethod(); };
        String result = response.getStatus() >= 400 || ex != null ? "FAILURE" : "SUCCESS";
        try { audit.record(username, action, "HTTP_REQUEST", request.getRequestURI(), clientIp(request), result, null, null, null); }
        catch (RuntimeException ignored) { /* Auditing cannot hide the original response. DB trigger still protects stored events. */ }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
