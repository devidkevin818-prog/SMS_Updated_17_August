package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.SignatureChangeProposal;
import com.bank.signaturemanagement.entity.SignatureUploadBatch;
import com.bank.signaturemanagement.entity.SignatureVersion;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.SignatureUploadBatchRepository;
import com.bank.signaturemanagement.repository.SignatureVersionRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SignatureWorkflowService {

 public record Preview(
         SignatureUploadBatch batch,
         List<SignatureVersion> matched,
         List<String> errors
 ) {
 }

 private static final Pattern FILE = Pattern.compile(
         "^(\\d{6})LOCAL|FOREIGN(?:[_-].*)?\\.[A-Za-z0-9]+$",
         Pattern.CASE_INSENSITIVE
 );

 private final SignatureVersionRepository versions;
 private final SignatureUploadBatchRepository batches;
 private final EmployeeRepository employees;
 private final UserRepository users;
 private final FileStorageService files;
 private final AccessControlService access;
 private final AuditService audit;
 private final SignatureChangeProposalService changes;

 public SignatureWorkflowService(
         SignatureVersionRepository versions,
         SignatureUploadBatchRepository batches,
         EmployeeRepository employees,
         UserRepository users,
         FileStorageService files,
         AccessControlService access,
         AuditService audit,
         SignatureChangeProposalService changes
 ) {
  this.versions = versions;
  this.batches = batches;
  this.employees = employees;
  this.users = users;
  this.files = files;
  this.access = access;
  this.audit = audit;
  this.changes = changes;
 }

 @Transactional
 public SignatureVersion submit(
         Long employeeId,
         String type,
         MultipartFile file,
         String username
 ) {
  return submit(
          employeeId,
          type,
          file,
          username,
          null
  );
 }

 @Transactional
 public SignatureVersion submit(
         Long employeeId,
         String requestedType,
         MultipartFile file,
         String username,
         Long proposalId
 ) {
  requireMaker(username);

  Employee employee = employees.findById(employeeId)
          .orElseThrow(() ->
                  new IllegalArgumentException(
                          "Employee not found"
                  )
          );

  if (!employee.isActive()) {
   throw new IllegalArgumentException(
           "Employee is not active"
   );
  }

  String type = normalize(requestedType);

  if (versions
          .existsByEmployeeNumberAndSignatureTypeAndStatusIn(
                  employee.getEmployeeNumber(),
                  type,
                  List.of(
                          "PENDING_DGM",
                          "PENDING_GM"
                  )
          )) {

   throw new IllegalArgumentException(
           "This signature type already has a pending "
                   + "approval request"
   );
  }

  SignatureChangeProposal proposal = null;

  if (changes.has(employee, type)) {
   if (proposalId == null) {
    throw new IllegalArgumentException(
            "Level 1 Checker or Level 2 Checker must initiate "
                    + "replacement of an existing "
                    + type
                    + " signature"
    );
   }

   proposal = changes.requireForUpload(
           proposalId,
           employeeId,
           type,
           username
   );

  } else if (proposalId != null) {
   throw new IllegalArgumentException(
           "No initiation request is needed for a missing signature"
   );
  }

  SignatureVersion version = newVersion(
          employee,
          type,
          file,
          username,
          null,
          "PENDING_DGM"
  );

  version.setChangeProposal(proposal);

  if (proposal != null) {
   changes.markSubmitted(
           proposal,
           version
   );
  }

  audit.record(
          username,
          proposal == null
                  ? "SIGNATURE_SUBMIT"
                  : "SIGNATURE_REPLACEMENT_SUBMIT",
          "SIGNATURE_VERSION",
          String.valueOf(version.getId()),
          null,
          "SUCCESS",
          null,
          version.getStatus(),
          employee.getEmployeeNumber()
                  + " "
                  + version.getSignatureType()
  );

  return version;
 }

 @Transactional
 public Preview preview(
         MultipartFile[] uploadFiles,
         String username
 ) {
  requireMaker(username);

  SignatureUploadBatch batch =
          new SignatureUploadBatch();

  batch.setBatchNumber(
          "SIG-"
                  + Year.now().getValue()
                  + "-"
                  + UUID.randomUUID()
                  .toString()
                  .substring(0, 8)
                  .toUpperCase(Locale.ROOT)
  );

  batch.setSubmittedBy(
          user(username)
  );

  batch.setStatus("DRAFT");

  batches.saveAndFlush(batch);

  List<SignatureVersion> matched =
          new ArrayList<>();

  List<String> errors =
          new ArrayList<>();

  MultipartFile[] filesToProcess =
          uploadFiles == null
                  ? new MultipartFile[0]
                  : uploadFiles;

  for (MultipartFile file : filesToProcess) {
   batch.setTotalFiles(
           batch.getTotalFiles() + 1
   );

   String name = file.getOriginalFilename() == null
           ? ""
           : file.getOriginalFilename();

   Matcher matcher =
           FILE.matcher(name);

   if (!matcher.matches()) {
    errors.add(
            name
                    + ": expected 123456_LOCAL.ext "
                    + "or 123456_FOREIGN.ext"
    );

    continue;
   }

   Optional<Employee> employee =
           employees.findByEmployeeNumber(
                   matcher.group(1)
           );

   if (employee.isEmpty()
           || !employee.get().isActive()) {

    errors.add(
            name + ": no active employee match"
    );

    continue;
   }

   try {
    String matchedType =
            normalize(matcher.group(2));

    if (changes.has(
            employee.get(),
            matchedType
    )) {
     throw new IllegalArgumentException(
             "existing "
                     + matchedType
                     + " signature requires Level 1 Checker "
                     + "or Level 2 Checker initiation and "
                     + "individual upload"
     );
    }

    matched.add(
            newVersion(
                    employee.get(),
                    matchedType,
                    file,
                    username,
                    batch,
                    "DRAFT"
            )
    );

   } catch (IllegalArgumentException exception) {
    errors.add(
            name + ": " + exception.getMessage()
    );
   }
  }

  batch.setMatchedFiles(
          matched.size()
  );

  batch.setInvalidFiles(
          errors.size()
  );

  batches.save(batch);

  audit.record(
          username,
          "SIGNATURE_BATCH_PREVIEW",
          "SIGNATURE_UPLOAD_BATCH",
          batch.getId().toString(),
          null,
          "SUCCESS",
          null,
          "matched=" + matched.size(),
          "invalid=" + errors.size()
  );

  return new Preview(
          batch,
          matched,
          errors
  );
 }

 @Transactional
 public void submitBatch(
         Long batchId,
         String username
 ) {
  requireMaker(username);

  SignatureUploadBatch batch =
          batches.findById(batchId)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "Signature batch not found"
                          )
                  );

  if (!batch.getSubmittedBy()
          .getUsername()
          .equals(username)
          && !isAdmin(username)) {

   throw new AccessDeniedException(
           "This is another user's batch"
   );
  }

  if (!"DRAFT".equals(batch.getStatus())) {
   throw new IllegalStateException(
           "Batch has already been submitted"
   );
  }

  if (batch.getInvalidFiles() > 0) {
   throw new IllegalArgumentException(
           "Correct all unmatched or invalid files "
                   + "before submission"
   );
  }

  List<SignatureVersion> rows =
          versions.findAll()
                  .stream()
                  .filter(version ->
                          version.getBatch() != null
                                  && version.getBatch()
                                  .getId()
                                  .equals(batchId)
                  )
                  .toList();

  if (rows.isEmpty()) {
   throw new IllegalArgumentException(
           "No matched signature files"
   );
  }

  rows.forEach(version ->
          version.setStatus("PENDING_DGM")
  );

  batch.setStatus("PENDING_DGM");

  audit.record(
          username,
          "SIGNATURE_BATCH_SUBMIT",
          "SIGNATURE_UPLOAD_BATCH",
          batchId.toString(),
          null,
          "SUCCESS",
          "DRAFT",
          "PENDING_DGM",
          null
  );
 }

 @Transactional(readOnly = true)
 public List<SignatureVersion> pending(
         String level
 ) {
  String workflowLevel =
          workflowLevel(level);

  return versions.findByStatusOrderBySubmittedAtAsc(
          "PENDING_" + workflowLevel
  );
 }

 @Transactional(readOnly = true)
 public List<SignatureVersion> pendingForPd() {
  List<SignatureVersion> result =
          new ArrayList<>();

  result.addAll(
          versions.findByStatusOrderBySubmittedAtAsc(
                  "PENDING_DGM"
          )
  );

  result.addAll(
          versions.findByStatusOrderBySubmittedAtAsc(
                  "PENDING_GM"
          )
  );

  return result;
 }

 @Transactional(readOnly = true)
 public List<SignatureVersion> history(
         String employeeNumber
 ) {
  return versions
          .findByEmployeeNumberOrderBySignatureTypeAscVersionNumberDesc(
                  employeeNumber
          );
 }

 @Transactional(readOnly = true)
 public Optional<SignatureVersion> current(
         String employeeNumber,
         String type
 ) {
  return versions
          .findByEmployeeNumberAndSignatureTypeAndCurrentApprovedTrue(
                  employeeNumber,
                  normalize(type)
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

  if (!Set.of(
          "APPROVE",
          "REJECT"
  ).contains(normalizedAction)) {
   throw new IllegalArgumentException(
           "Invalid decision"
   );
  }

  /*
   * Existing permission keys are retained:
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

  SignatureVersion version =
          versions.findById(id)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "Signature request not found"
                          )
                  );

  String expectedStatus =
          "PENDING_" + workflowLevel;

  if (!expectedStatus.equals(
          version.getStatus()
  )) {
   throw new IllegalStateException(
           "Signature is not awaiting "
                   + checkerLabel(role)
   );
  }

  if ("REJECT".equals(normalizedAction)
          && (remarks == null || remarks.isBlank())) {

   throw new IllegalArgumentException(
           "Rejection remarks are required"
   );
  }

  User actor =
          user(username);

  LocalDateTime now =
          LocalDateTime.now();

  if ("LEVEL_1_CHECKER".equals(role)) {
   version.setDgmApprover(actor);
   version.setDgmDecidedAt(now);
   version.setDgmRemarks(
           clean(remarks)
   );
  } else {
   version.setGmApprover(actor);
   version.setGmDecidedAt(now);
   version.setGmRemarks(
           clean(remarks)
   );
  }

  if ("REJECT".equals(normalizedAction)) {
   version.setStatus("REJECTED");

   version.setRejectionRemarks(
           remarks.trim()
   );

  } else if ("LEVEL_1_CHECKER".equals(role)) {
   version.setStatus("PENDING_GM");

  } else {
   activate(version);
  }

  changes.syncDecision(version);

  audit.record(
          username,
          "SIGNATURE_"
                  + workflowLevel
                  + "_"
                  + normalizedAction,
          "SIGNATURE_VERSION",
          id.toString(),
          null,
          "SUCCESS",
          null,
          version.getStatus(),
          remarks
  );
 }

 @Transactional
 public void decideBatch(
         Long batchId,
         String level,
         String action,
         String remarks,
         Set<Long> rejectIds,
         String username
 ) {
  String role =
          normalizeCheckerRole(level);

  String workflowLevel =
          workflowLevel(role);

  List<SignatureVersion> rows =
          versions.findAll()
                  .stream()
                  .filter(version ->
                          version.getBatch() != null
                                  && version.getBatch()
                                  .getId()
                                  .equals(batchId)
                                  && version.getStatus()
                                  .equals(
                                          "PENDING_"
                                                  + workflowLevel
                                  )
                  )
                  .toList();

  if (rows.isEmpty()) {
   throw new IllegalArgumentException(
           "No pending rows in this batch"
   );
  }

  for (SignatureVersion row : rows) {
   String rowAction =
           rejectIds != null
                   && rejectIds.contains(row.getId())
                   ? "REJECT"
                   : action;

   decide(
           row.getId(),
           role,
           rowAction,
           remarks,
           username
   );
  }

  SignatureUploadBatch batch =
          batches.findById(batchId)
                  .orElseThrow(() ->
                          new IllegalArgumentException(
                                  "Signature batch not found"
                          )
                  );

  batch.setStatus(
          rows.stream()
                  .anyMatch(version ->
                          "PENDING_GM".equals(
                                  version.getStatus()
                          )
                  )
                  ? "PENDING_GM"
                  : "DECIDED"
  );
 }

 private void activate(
         SignatureVersion version
 ) {
  Employee employee =
          version.getEmployee();

  String organized =
          files.organizeEmployeeImage(
                  version.getFilePath(),
                  "LOCAL".equals(
                          version.getSignatureType()
                  )
                          ? "signature"
                          : "foreign-signature",
                  employee.getId()
          );

  version.setFilePath(organized);

  versions.clearCurrent(
          version.getEmployeeNumber(),
          version.getSignatureType()
  );

  version.setCurrentApproved(true);
  version.setStatus("APPROVED");

  if ("LOCAL".equals(
          version.getSignatureType()
  )) {
   employee.setSignaturePath(organized);
  } else {
   employee.setForeignSignaturePath(organized);
  }
 }

 private SignatureVersion newVersion(
         Employee employee,
         String type,
         MultipartFile file,
         String username,
         SignatureUploadBatch batch,
         String status
 ) {
  String normalizedType =
          normalize(type);

  files.validateImage(file);

  SignatureVersion version =
          new SignatureVersion();

  version.setEmployee(employee);

  version.setEmployeeNumber(
          employee.getEmployeeNumber()
  );

  version.setSignatureType(
          normalizedType
  );

  version.setVersionNumber(
          (int) versions
                  .countByEmployeeNumberAndSignatureType(
                          employee.getEmployeeNumber(),
                          normalizedType
                  ) + 1
  );

  version.setFilePath(
          files.storeImage(
                  file,
                  "pending-"
                          + normalizedType
                          .toLowerCase(Locale.ROOT)
                          + "-signature"
          )
  );

  version.setSubmittedBy(
          user(username)
  );

  version.setBatch(batch);
  version.setStatus(status);

  return versions.saveAndFlush(version);
 }

 private String normalize(
         String type
 ) {
  String normalized =
          type == null
                  ? ""
                  : type.trim()
                  .toUpperCase(Locale.ROOT);

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

 private User user(
         String username
 ) {
  return users.findByUsername(username)
          .orElseThrow(() ->
                  new IllegalArgumentException(
                          "User not found"
                  )
          );
 }

 private void requireMaker(
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
 }

 /*
  * Converts the new application roles to the existing workflow
  * status and permission identifiers.
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
  * Accepting DGM and GM here keeps older callers compatible.
  * All authorization checks still use the new role names.
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

 private boolean isAdmin(
         String username
 ) {
  return access.hasAnyRole(
          username,
          "ADMIN"
  );
 }

 private String clean(
         String value
 ) {
  return value == null
          ? null
          : value.trim();
 }
}
