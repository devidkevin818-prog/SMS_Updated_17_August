package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.*; import org.junit.jupiter.api.Test; import org.springframework.security.access.AccessDeniedException; import org.springframework.security.core.Authentication; import jakarta.servlet.http.HttpServletRequest; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;

class SignatureBookControllerTest {
 @Test void craftedDownloadIsRejectedForNonPdRole() throws Exception {
  SignatureBookService books=mock(SignatureBookService.class);AccessControlService access=mock(AccessControlService.class);AuditService audit=mock(AuditService.class);Authentication auth=mock(Authentication.class);HttpServletRequest request=mock(HttpServletRequest.class);
  when(auth.getName()).thenReturn("dgm-user");when(access.hasAnyRole("dgm-user","PD","ADMIN")).thenReturn(false);
  SignatureBookController controller=new SignatureBookController(books,access,audit,mock(SignatureBookPublicationService.class),mock(com.bank.signaturemanagement.repository.SignatureBookEntryRepository.class),mock(FileStorageService.class));
  assertThrows(AccessDeniedException.class,()->controller.content(1L,true,auth,request));
  verifyNoInteractions(books,audit);
 }
}
