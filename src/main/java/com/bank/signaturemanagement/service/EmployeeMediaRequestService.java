package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeMediaRequest;
import com.bank.signaturemanagement.entity.EmployeeMediaVersion;
import com.bank.signaturemanagement.entity.SignatureVersion;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.EmployeeMediaRequestRepository;
import com.bank.signaturemanagement.repository.EmployeeMediaVersionRepository;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.SignatureVersionRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeMediaRequestService {

 private final EmployeeMediaRequestRepository requests;
 private final EmployeeRepository employees;
 private final UserRepository users;
 private final FileStorageService files;
 private final AccessControlService access;
 private final AuditService audit;
 private final SignatureVersionRepository signatures;
 private final EmployeeMediaVersionRepository mediaVersions;

 public EmployeeMediaRequestService(
         EmployeeMediaRequestRepository requests,
         EmployeeRepository employees,
         UserRepository users,
         FileStorageService files,
         AccessControlService access,
         AuditService audit,
         SignatureVersionRepository signatures,
         EmployeeMediaVersionRepository mediaVersions
 ) {
  this.requests = requests;
  this.employees = employees;
  this.users = users;
  this.files = files;
  this.access = access;
  this.audit = audit;
  this.signatures = signatures;
  this.mediaVersions = mediaVersions;
 }

 @Transactional
 public void submit(
         Long employeeId,
         MultipartFile photo,
         MultipartFile local,
         MultipartFile foreign,
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

  Employee employee = employees.findById(employeeId)
          .orElseThrow(() ->
                  new IllegalArgumentException(
                          "Employee not found"
                  )
          );

  if (requests.existsByEmployeeIdAndStatusIn(
          employeeId,
          List.of(
                  "PENDING_DGM",
                  "PENDING_GM"
          )
  )) {
   throw new IllegalArgumentException(
           "This employee already has a pending media request"
   );
  }

  boolean photoPresent = present(photo);
  boolean localPresent = present(local);
  boolean foreignPresent = present(foreign);

  if (!photoPresent
          && !localPresent
          && !foreignPresent) {

   throw new IllegalArgumentException(
           "Upload at least one profile or signature image"
   );
  }

  if (photoPresent
          && employee.getPhotoPath() != null) {

   throw new IllegalArgumentException(
           "Existing profile photo changes require an initiated "
                   + "employee update"
   );
  }

  if (localPresent
          && employee.getSignaturePath() != null) {

   throw new IllegalArgumentException(
           "Existing LOCAL signature requires Level 1 Checker "
                   + "or Level 2 Checker initiation"
   );
  }

  if (foreignPresent
          && employee.getForeignSignaturePath() != null) {

   throw new IllegalArgumentException(
           "Existing FOREIGN signature requires Level 1 Checker "
                   + "or Level 2 Checker initiation"
   );
  }

  EmployeeMediaRequest mediaRequest =
          new EmployeeMediaRequest();

  mediaRequest.setEmployee(employee);

  mediaRequest.setSubmittedBy(
          users.findByUsername(username)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "User not found"
                          )
                  )
  );

  if (photoPresent) {
   mediaRequest.setPhotoPath(
           store(
                   photo,
                   "pending-photo"
           )
   );
  }

  if (localPresent) {
   mediaRequest.setLocalSignaturePath(
           store(
                   local,
                   "pending-local-signature"
           )
   );
  }

  if (foreignPresent) {
   mediaRequest.setForeignSignaturePath(
           store(
                   foreign,
                   "pending-foreign-signature"
           )
   );
  }

  requests.saveAndFlush(mediaRequest);

  audit.record(
          username,
          "EMPLOYEE_MEDIA_SUBMIT",
          "EMPLOYEE_MEDIA_REQUEST",
          String.valueOf(mediaRequest.getId()),
          null,
          "SUCCESS",
          null,
          "PENDING_DGM",
          "photo=" + photoPresent
                  + ",local=" + localPresent
                  + ",foreign=" + foreignPresent
  );
 }

 @Transactional(readOnly = true)
 public List<EmployeeMediaRequest> pending(
         String level
 ) {
  String workflowLevel =
          workflowLevel(level);

  return requests.findByStatusOrderBySubmittedAtAsc(
          "PENDING_" + workflowLevel
  );
 }

 @Transactional
 public void decide(
         Long id,
         String level,
         String action,
         String remarks,
         String username
 ) {
  String role =
          normalizeCheckerRole(level);

  String workflowLevel =
          workflowLevel(role);

  String normalizedAction =
          action == null
                  ? ""
                  : action.trim().toUpperCase(Locale.ROOT);

  /*
   * Legacy permission names remain unchanged:
   * APPROVE_DGM and APPROVE_GM.
   */
  access.require(
          username,
          "APPROVE_" + workflowLevel
  );

  if (!access.hasAnyRole(
          username,
          role,
          "ADMIN"
  )) {
   throw new AccessDeniedException(
           "Wrong approval level"
   );
  }

  EmployeeMediaRequest mediaRequest =
          requests.findById(id)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "Media request not found"
                          )
                  );

  String expectedStatus =
          "PENDING_" + workflowLevel;

  if (!expectedStatus.equals(
          mediaRequest.getStatus()
  )) {
   throw new IllegalStateException(
           "Request is not awaiting "
                   + checkerLabel(role)
   );
  }

  if ("REJECT".equals(normalizedAction)
          && (remarks == null || remarks.isBlank())) {

   throw new IllegalArgumentException(
           "Rejection remarks are required"
   );
  }

  User actor = users.findByUsername(username)
          .orElseThrow(() ->
                  new IllegalArgumentException(
                          "User not found"
                  )
          );

  LocalDateTime now =
          LocalDateTime.now();

  /*
   * Legacy entity field names remain unchanged for database
   * compatibility.
   */
  if ("LEVEL_1_CHECKER".equals(role)) {
   mediaRequest.setDgmApprover(actor);
   mediaRequest.setDgmDecidedAt(now);
   mediaRequest.setDgmRemarks(
           clean(remarks)
   );
  } else {
   mediaRequest.setGmApprover(actor);
   mediaRequest.setGmDecidedAt(now);
   mediaRequest.setGmRemarks(
           clean(remarks)
   );
  }

  if ("REJECT".equals(normalizedAction)) {
   mediaRequest.setStatus("REJECTED");

   mediaRequest.setRejectionRemarks(
           remarks.trim()
   );

  } else if ("LEVEL_1_CHECKER".equals(role)) {
   mediaRequest.setStatus("PENDING_GM");

  } else {
   activate(
           mediaRequest,
           actor
   );
  }

  audit.record(
          username,
          "EMPLOYEE_MEDIA_"
                  + workflowLevel
                  + "_"
                  + normalizedAction,
          "EMPLOYEE_MEDIA_REQUEST",
          id.toString(),
          null,
          "SUCCESS",
          null,
          mediaRequest.getStatus(),
          remarks
  );
 }

 private void activate(
         EmployeeMediaRequest mediaRequest,
         User level2Checker
 ) {
  Employee employee =
          mediaRequest.getEmployee();

  if (mediaRequest.getPhotoPath() != null) {
   employee.setPhotoPath(
           files.organizeEmployeeImage(
                   mediaRequest.getPhotoPath(),
                   "profile",
                   employee.getId()
           )
   );
  }

  if (mediaRequest.getLocalSignaturePath() != null) {
   String path =
           files.organizeEmployeeImage(
                   mediaRequest.getLocalSignaturePath(),
                   "signature",
                   employee.getId()
           );

   employee.setSignaturePath(path);

   saveSignature(
           employee,
           "LOCAL",
           path,
           mediaRequest.getSubmittedBy(),
           mediaRequest.getDgmApprover(),
           level2Checker
   );
  }

  if (mediaRequest.getForeignSignaturePath() != null) {
   String path =
           files.organizeEmployeeImage(
                   mediaRequest.getForeignSignaturePath(),
                   "foreign-signature",
                   employee.getId()
           );

   employee.setForeignSignaturePath(path);

   saveSignature(
           employee,
           "FOREIGN",
           path,
           mediaRequest.getSubmittedBy(),
           mediaRequest.getDgmApprover(),
           level2Checker
   );
  }

  EmployeeMediaVersion mediaVersion =
          new EmployeeMediaVersion();

  mediaVersion.setEmployee(employee);

  mediaVersion.setVersionNumber(
          (int) mediaVersions.countByEmployeeId(
                  employee.getId()
          ) + 1
  );

  mediaVersion.setPhotoPath(
          employee.getPhotoPath()
  );

  mediaVersion.setSignaturePath(
          employee.getSignaturePath()
  );

  mediaVersion.setForeignSignaturePath(
          employee.getForeignSignaturePath()
  );

  mediaVersions.save(mediaVersion);

  mediaRequest.setStatus("APPROVED");
 }

 private void saveSignature(
         Employee employee,
         String type,
         String path,
         User submitter,
         User level1Checker,
         User level2Checker
 ) {
  signatures.clearCurrent(
          employee.getEmployeeNumber(),
          type
  );

  SignatureVersion version =
          new SignatureVersion();

  version.setEmployee(employee);

  version.setEmployeeNumber(
          employee.getEmployeeNumber()
  );

  version.setSignatureType(type);

  version.setVersionNumber(
          (int) signatures
                  .countByEmployeeNumberAndSignatureType(
                          employee.getEmployeeNumber(),
                          type
                  ) + 1
  );

  version.setFilePath(path);
  version.setSubmittedBy(submitter);

  /*
   * Legacy DGM and GM entity properties represent the Level 1
   * Checker and Level 2 Checker approval records.
   */
  version.setDgmApprover(level1Checker);
  version.setDgmDecidedAt(
          level1Checker != null
                  ? LocalDateTime.now()
                  : null
  );

  version.setGmApprover(level2Checker);
  version.setGmDecidedAt(
          LocalDateTime.now()
  );

  version.setStatus("APPROVED");
  version.setCurrentApproved(true);

  signatures.save(version);
 }

 /*
  * Converts the new checker roles to the existing database workflow
  * status and permission suffixes.
  */
 private String workflowLevel(
         String level
 ) {
  String role =
          normalizeCheckerRole(level);

  return switch (role) {
   case "LEVEL_1_CHECKER" -> "DGM";
   case "LEVEL_2_CHECKER" -> "GM";
   default -> throw new IllegalArgumentException(
           "Invalid checker level"
   );
  };
 }

 /*
  * DGM and GM remain accepted temporarily for compatibility with
  * older callers. Authorization uses the new role names.
  */
 private String normalizeCheckerRole(
         String level
 ) {
  if (level == null || level.isBlank()) {
   throw new IllegalArgumentException(
           "Approval level is required"
   );
  }

  String normalized =
          level.trim().toUpperCase(Locale.ROOT);

  return switch (normalized) {
   case "LEVEL_1_CHECKER", "DGM" ->
           "LEVEL_1_CHECKER";

   case "LEVEL_2_CHECKER", "GM" ->
           "LEVEL_2_CHECKER";

   default ->
           throw new IllegalArgumentException(
                   "Invalid checker level"
           );
  };
 }

 private String checkerLabel(
         String role
 ) {
  return switch (role) {
   case "LEVEL_1_CHECKER" ->
           "Level 1 Checker";

   case "LEVEL_2_CHECKER" ->
           "Level 2 Checker";

   default ->
           throw new IllegalArgumentException(
                   "Invalid checker role"
           );
  };
 }

 private String store(
         MultipartFile file,
         String folder
 ) {
  files.validateImage(file);

  return files.storeImage(
          file,
          folder
  );
 }

 private boolean present(
         MultipartFile file
 ) {
  return file != null
          && !file.isEmpty();
 }

 private String clean(
         String value
 ) {
  return value == null
          ? null
          : value.trim();
 }
}