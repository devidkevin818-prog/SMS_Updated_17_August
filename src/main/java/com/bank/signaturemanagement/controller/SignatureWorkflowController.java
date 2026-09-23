package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.EmployeeMediaRequestService;
import com.bank.signaturemanagement.service.EmployeeService;
import com.bank.signaturemanagement.service.SignatureChangeProposalService;
import com.bank.signaturemanagement.service.SignatureWorkflowService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SignatureWorkflowController {

 private final SignatureWorkflowService service;
 private final EmployeeService employees;
 private final SignatureChangeProposalService changes;
 private final EmployeeMediaRequestService mediaRequests;

 public SignatureWorkflowController(
         SignatureWorkflowService service,
         EmployeeService employees,
         SignatureChangeProposalService changes,
         EmployeeMediaRequestService mediaRequests
 ) {
  this.service = service;
  this.employees = employees;
  this.changes = changes;
  this.mediaRequests = mediaRequests;
 }

 @GetMapping("/pd/signatures")
 public String pd(
         @RequestParam(defaultValue = "") String query,
         @RequestParam(defaultValue = "0") int page,
         Authentication authentication,
         Model model
 ) {
  var employeePage = employees.search(query, page);

  Map<String, String> pending = new HashMap<>();

  for (var version : service.pendingForPd()) {
   pending.put(
           version.getEmployeeNumber()
                   + ":"
                   + version.getSignatureType(),
           version.getStatus()
   );
  }

  Map<Long, String> mediaPending = new HashMap<>();

  for (var request : mediaRequests.pending("LEVEL_1_CHECKER")) {
   mediaPending.put(
           request.getEmployee().getId(),
           "PENDING_DGM"
   );
  }

  for (var request : mediaRequests.pending("LEVEL_2_CHECKER")) {
   mediaPending.put(
           request.getEmployee().getId(),
           "PENDING_GM"
   );
  }

  List<PdSignatureRow> rows = employeePage
          .getContent()
          .stream()
          .map(employee -> new PdSignatureRow(
                  employee,
                  pending.get(
                          employee.getEmployeeNumber()
                                  + ":LOCAL"
                  ),
                  pending.get(
                          employee.getEmployeeNumber()
                                  + ":FOREIGN"
                  ),
                  mediaPending.get(employee.getId())
          ))
          .filter(PdSignatureRow::actionable)
          .toList();

  model.addAttribute(
          "query",
          query
  );

  model.addAttribute(
          "employeeRows",
          rows
  );

  model.addAttribute(
          "replacementRequests",
          changes.pendingPd(authentication.getName())
  );

  return "pd/signature-upload";
 }

 @PostMapping("/pd/employees/{id}/signatures")
 public String upload(
         @PathVariable Long id,
         @RequestParam String type,
         @RequestParam(required = false) Long proposalId,
         @RequestParam MultipartFile file,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   service.submit(
           id,
           type,
           file,
           authentication.getName(),
           proposalId
   );

   redirectAttributes.addFlashAttribute(
           "success",
           "Signature submitted to Level 1 Checker"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/pd/signatures";
 }

 @PostMapping("/pd/employees/{id}/media")
 public String uploadMedia(
         @PathVariable Long id,
         @RequestParam(required = false) MultipartFile photo,
         @RequestParam(required = false) MultipartFile localSignature,
         @RequestParam(required = false) MultipartFile foreignSignature,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   mediaRequests.submit(
           id,
           photo,
           localSignature,
           foreignSignature,
           authentication.getName()
   );

   redirectAttributes.addFlashAttribute(
           "success",
           "Employee media submitted to Level 1 Checker"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/pd/signatures";
 }

 @PostMapping("/pd/signatures/bulk/preview")
 public String preview(
         @RequestParam("files") MultipartFile[] files,
         Authentication authentication,
         Model model
 ) {
  var preview = service.preview(
          files,
          authentication.getName()
  );

  model.addAttribute(
          "preview",
          preview
  );

  return "pd/signature-bulk-preview";
 }

 @PostMapping("/pd/signatures/bulk/{id}/submit")
 public String submitBatch(
         @PathVariable Long id,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   service.submitBatch(
           id,
           authentication.getName()
   );

   redirectAttributes.addFlashAttribute(
           "success",
           "Signature batch submitted to Level 1 Checker"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/pd/signatures";
 }

 @PostMapping("/{level:dgm|gm}/signature-requests/{id}/decision")
 public String decide(
         @PathVariable String level,
         @PathVariable Long id,
         @RequestParam String action,
         @RequestParam(required = false) String remarks,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   service.decide(
           id,
           checkerRole(level),
           action,
           remarks,
           authentication.getName()
   );

   redirectAttributes.addFlashAttribute(
           "success",
           "Signature decision saved"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/" + level + "/dashboard";
 }

 @PostMapping("/{level:dgm|gm}/media-requests/{id}/decision")
 public String decideMedia(
         @PathVariable String level,
         @PathVariable Long id,
         @RequestParam String action,
         @RequestParam(required = false) String remarks,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   mediaRequests.decide(
           id,
           checkerRole(level),
           action,
           remarks,
           authentication.getName()
   );

   redirectAttributes.addFlashAttribute(
           "success",
           "Media decision saved"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/" + level + "/dashboard";
 }

 @GetMapping("/{level:dgm|gm}/signature-requests")
 public String inbox(
         @PathVariable String level,
         Model model
 ) {
  model.addAttribute(
          "level",
          checkerLabel(level)
  );

  model.addAttribute(
          "requests",
          service.pending(checkerRole(level))
  );

  return "signature-approvals";
 }

 @GetMapping("/{level:dgm|gm}/signature-management")
 public String management(
         @PathVariable String level,
         @RequestParam(defaultValue = "") String query,
         @RequestParam(defaultValue = "0") int page,
         Model model
 ) {
  model.addAttribute(
          "level",
          checkerLabel(level)
  );

  model.addAttribute(
          "query",
          query
  );

  model.addAttribute(
          "employees",
          employees.search(query, page)
  );

  return "signature-management";
 }

 @PostMapping("/{level:dgm|gm}/employees/{id}/signature-change")
 public String initiate(
         @PathVariable String level,
         @PathVariable Long id,
         @RequestParam String type,
         @RequestParam String remarks,
         Authentication authentication,
         RedirectAttributes redirectAttributes
 ) {
  try {
   changes.initiate(
           id,
           type,
           remarks,
           authentication.getName()
   );

   redirectAttributes.addFlashAttribute(
           "success",
           type + " signature replacement sent to Maker"
   );
  } catch (RuntimeException exception) {
   redirectAttributes.addFlashAttribute(
           "error",
           exception.getMessage()
   );
  }

  return "redirect:/" + level + "/signature-management";
 }

 private String checkerRole(String level) {
  if ("dgm".equalsIgnoreCase(level)) {
   return "LEVEL_1_CHECKER";
  }

  if ("gm".equalsIgnoreCase(level)) {
   return "LEVEL_2_CHECKER";
  }

  throw new IllegalArgumentException(
          "Invalid checker level"
  );
 }

 private String checkerLabel(String level) {
  if ("dgm".equalsIgnoreCase(level)) {
   return "Level 1 Checker";
  }

  if ("gm".equalsIgnoreCase(level)) {
   return "Level 2 Checker";
  }

  throw new IllegalArgumentException(
          "Invalid checker level"
  );
 }

 public record PdSignatureRow(
         com.bank.signaturemanagement.entity.Employee employee,
         String localPending,
         String foreignPending,
         String mediaPending
 ) {
  public boolean actionable() {
   return employee.getPhotoPath() == null
           || employee.getSignaturePath() == null
           || employee.getForeignSignaturePath() == null
           || localPending != null
           || foreignPending != null
           || mediaPending != null;
  }

  public String pendingLabel(String status) {
   return status == null
           ? null
           : status.replace(
                   "PENDING_DGM",
                   "Pending Level 1 Checker"
           )
           .replace(
                   "PENDING_GM",
                   "Pending Level 2 Checker"
           )
           .replace(
                   "PENDING_",
                   "Pending "
           )
           .replace('_', ' ');
  }
 }
}