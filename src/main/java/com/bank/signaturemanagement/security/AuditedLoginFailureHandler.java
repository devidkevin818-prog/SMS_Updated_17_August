package com.bank.signaturemanagement.security;
import com.bank.signaturemanagement.service.AuditService;
import jakarta.servlet.ServletException; import jakarta.servlet.http.*; import org.springframework.security.core.AuthenticationException; import org.springframework.security.web.authentication.AuthenticationFailureHandler; import org.springframework.stereotype.Component; import java.io.IOException;
@Component public class AuditedLoginFailureHandler implements AuthenticationFailureHandler{
 private final AuditService audit; public AuditedLoginFailureHandler(AuditService audit){this.audit=audit;}
 @Override public void onAuthenticationFailure(HttpServletRequest request,HttpServletResponse response,AuthenticationException exception)throws IOException,ServletException{
  audit.record(request.getParameter("username"),"LOGIN","SESSION",null,request.getRemoteAddr(),"FAILURE",null,null,"Invalid credentials or inactive account"); response.sendRedirect(request.getContextPath()+"/login?error");
 }
}
