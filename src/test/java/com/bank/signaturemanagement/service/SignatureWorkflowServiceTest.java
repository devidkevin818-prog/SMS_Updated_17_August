package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.*; import com.bank.signaturemanagement.repository.*; import org.junit.jupiter.api.Test; import org.springframework.mock.web.MockMultipartFile; import java.util.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;

class SignatureWorkflowServiceTest {
 @Test void submittingReplacementCreatesANewPendingVersionWithoutChangingEmployee(){
  SignatureVersionRepository versions=mock(SignatureVersionRepository.class);SignatureUploadBatchRepository batches=mock(SignatureUploadBatchRepository.class);EmployeeRepository employees=mock(EmployeeRepository.class);UserRepository users=mock(UserRepository.class);FileStorageService files=mock(FileStorageService.class);AccessControlService access=mock(AccessControlService.class);AuditService audit=mock(AuditService.class);
  Employee employee=new Employee();employee.setEmployeeNumber("123456");employee.setActive(true);employee.setSignaturePath("approved/old.png");User pd=new User();pd.setUsername("pd");when(access.hasAnyRole("pd","PD","ADMIN")).thenReturn(true);when(employees.findById(1L)).thenReturn(Optional.of(employee));when(users.findByUsername("pd")).thenReturn(Optional.of(pd));when(versions.countByEmployeeNumberAndSignatureType("123456","LOCAL")).thenReturn(3L);when(files.storeImage(any(),anyString())).thenReturn("pending/new.png");when(versions.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
  SignatureChangeProposalService changes=mock(SignatureChangeProposalService.class);SignatureWorkflowService service=new SignatureWorkflowService(versions,batches,employees,users,files,access,audit,changes);SignatureVersion result=service.submit(1L,"LOCAL",new MockMultipartFile("file","sig.png","image/png",new byte[]{1}),"pd");
  assertEquals(4,result.getVersionNumber());assertEquals("PENDING_DGM",result.getStatus());assertEquals("approved/old.png",employee.getSignaturePath());assertFalse(result.isCurrentApproved());
 }

 @Test void existingSignatureCannotBeReplacedWithoutDgmOrGmInitiation(){
  SignatureVersionRepository versions=mock(SignatureVersionRepository.class);SignatureUploadBatchRepository batches=mock(SignatureUploadBatchRepository.class);EmployeeRepository employees=mock(EmployeeRepository.class);UserRepository users=mock(UserRepository.class);FileStorageService files=mock(FileStorageService.class);AccessControlService access=mock(AccessControlService.class);AuditService audit=mock(AuditService.class);SignatureChangeProposalService changes=mock(SignatureChangeProposalService.class);
  Employee employee=new Employee();employee.setEmployeeNumber("123456");employee.setActive(true);employee.setSignaturePath("signature/1/approved.png");when(access.hasAnyRole("pd","PD","ADMIN")).thenReturn(true);when(employees.findById(1L)).thenReturn(Optional.of(employee));when(changes.has(employee,"LOCAL")).thenReturn(true);
  SignatureWorkflowService service=new SignatureWorkflowService(versions,batches,employees,users,files,access,audit,changes);
  var error=assertThrows(IllegalArgumentException.class,()->service.submit(1L,"LOCAL",new MockMultipartFile("file","sig.png","image/png",new byte[]{1}),"pd"));
  assertTrue(error.getMessage().contains("DGM or GM"));verifyNoInteractions(files);verify(versions).existsByEmployeeNumberAndSignatureTypeAndStatusIn(eq("123456"),eq("LOCAL"),anyCollection());verifyNoMoreInteractions(versions);
 }
}
