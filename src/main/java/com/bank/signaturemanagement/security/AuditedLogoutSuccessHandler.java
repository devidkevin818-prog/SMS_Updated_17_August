package com.bank.signaturemanagement.security;
import com.bank.signaturemanagement.service.AuditService; import jakarta.servlet.http.*; import org.springframework.security.core.Authentication; import org.springframework.security.web.authentication.logout.LogoutSuccessHandler; import org.springframework.stereotype.Component; import java.io.IOException;
@Component public class AuditedLogoutSuccessHandler implements LogoutSuccessHandler{
 private final AuditService audit; public AuditedLogoutSuccessHandler(AuditService audit){this.audit=audit;}
 @Override public void onLogoutSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException{
  audit.record(authentication==null?null:authentication.getName(),"LOGOUT","SESSION",request.getRequestedSessionId(),request.getRemoteAddr(),"SUCCESS",null,null,null); response.sendRedirect(request.getContextPath()+"/login?logout");
 }
}
