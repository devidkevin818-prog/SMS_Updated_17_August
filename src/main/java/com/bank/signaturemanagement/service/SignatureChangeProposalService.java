package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.SignatureChangeProposal;
import com.bank.signaturemanagement.entity.SignatureVersion;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.SignatureChangeProposalRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SignatureChangeProposalService {

 private static final List<String> OPEN = List.of(
         "PD_ACTION_REQUIRED",
         "PENDING_DGM",
         "PENDING_GM"
 );

 private final SignatureChangeProposalRepository proposals;
 private final EmployeeRepository employees;
 private final UserRepository users;
 private final AccessControlService access;
 private final AuditService audit;

 public SignatureChangeProposalService(
         SignatureChangeProposalRepository proposals,
         EmployeeRepository employees,
         UserRepository users,
         AccessControlService access,
         AuditService audit
 ) {
  this.proposals = proposals;
  this.employees = employees;
  this.users = users;
  this.access = access;
  this.audit = audit;
 }

 @Transactional
 public void initiate(
         Long employeeId,
         String requestedType,
         String remarks,
         String username
 ) {
  if (!access.hasAnyRole(
          username,
          "LEVEL_1_CHECKER",
          "LEVEL_2_CHECKER",
          "ADMIN"
  )) {
   throw new AccessDeniedException(
           "Only Level 1 Checker or Level 2 Checker may initiate "
                   + "a signature replacement"
   );
  }

  String type = type(requestedType);

  if (remarks == null || remarks.isBlank()) {
   throw new IllegalArgumentException(
           "Initiator remarks are required"
   );
  }

  Employee employee = employees.findById(employeeId)
          .orElseThrow(() ->
                  new IllegalArgumentException(
                          "Employee not found"
                  )
          );

  if (!employee.isActive()) {
   throw new IllegalArgumentException(
           "Employee is inactive"
   );
  }

  if (!has(employee, type)) {
   throw new IllegalArgumentException(
           "This signature type is missing; Maker can upload it "
                   + "directly without an initiation request"
   );
  }

  if (proposals.existsByEmployeeIdAndSignatureTypeAndStatusIn(
          employeeId,
          type,
          OPEN
  )) {
   throw new IllegalArgumentException(
           "An open replacement request already exists "
                   + "for this signature type"
   );
  }

  SignatureChangeProposal proposal =
          new SignatureChangeProposal();

  proposal.setEmployee(employee);
  proposal.setSignatureType(type);

  proposal.setInitiatedBy(
          users.findByUsername(username)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "User not found"
                          )
                  )
  );

  proposal.setInitiatorRemarks(
          remarks.trim()
  );

  proposals.saveAndFlush(proposal);

  audit.record(
          username,
          "SIGNATURE_CHANGE_INITIATE",
          "SIGNATURE_CHANGE_PROPOSAL",
          String.valueOf(proposal.getId()),
          null,
          "SUCCESS",
          null,
          "PD_ACTION_REQUIRED",
          employee.getEmployeeNumber()
                  + " "
                  + type
                  + ": "
                  + remarks.trim()
  );
 }

 @Transactional(readOnly = true)
 public List<SignatureChangeProposal> pendingPd(
         String username
 ) {
  if (!access.hasAnyRole(
          username,
          "MAKER",
          "ADMIN"
  )) {
   throw new AccessDeniedException(
           "Maker authority is required"
   );
  }

  return proposals.findByStatusOrderByCreatedAtAsc(
          "PD_ACTION_REQUIRED"
  );
 }

 @Transactional
 public SignatureChangeProposal requireForUpload(
         Long id,
         Long employeeId,
         String requestedType,
         String username
 ) {
  if (!access.hasAnyRole(
          username,
          "MAKER",
          "ADMIN"
  )) {
   throw new AccessDeniedException(
           "Maker authority is required"
   );
  }

  SignatureChangeProposal proposal =
          proposals.findById(id)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "Replacement request not found"
                          )
                  );

  if (!"PD_ACTION_REQUIRED".equals(
          proposal.getStatus()
  )) {
   throw new IllegalStateException(
           "Replacement request is no longer awaiting Maker"
   );
  }

  if (!proposal.getEmployee()
          .getId()
          .equals(employeeId)
          || !proposal.getSignatureType()
          .equals(type(requestedType))) {

   throw new IllegalArgumentException(
           "Replacement request does not match the employee "
                   + "and signature type"
   );
  }

  return proposal;
 }

 @Transactional
 public void markSubmitted(
         SignatureChangeProposal proposal,
         SignatureVersion version
 ) {
  proposal.setSubmittedVersion(version);
  proposal.setStatus("PENDING_DGM");
 }

 @Transactional
 public void syncDecision(
         SignatureVersion version
 ) {
  SignatureChangeProposal proposal =
          version.getChangeProposal();

  if (proposal == null) {
   return;
  }

  proposal.setStatus(
          switch (version.getStatus()) {
           case "PENDING_GM" -> "PENDING_GM";
           case "APPROVED" -> "EFFECTIVE";
           case "REJECTED" -> "REJECTED";
           default -> proposal.getStatus();
          }
  );
 }

 public boolean has(
         Employee employee,
         String type
 ) {
  return "LOCAL".equals(type)
          ? employee.getSignaturePath() != null
            && !employee.getSignaturePath().isBlank()
          : employee.getForeignSignaturePath() != null
            && !employee.getForeignSignaturePath().isBlank();
 }

 public String type(
         String value
 ) {
  String normalized = value == null
          ? ""
          : value.trim().toUpperCase(Locale.ROOT);

  if (!Set.of(
          "LOCAL",
          "FOREIGN"
  ).contains(normalized)) {
   throw new IllegalArgumentException(
           "Signature type must be LOCAL or FOREIGN"
   );
  }

  return normalized;
 }
}