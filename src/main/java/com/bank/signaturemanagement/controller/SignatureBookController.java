package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.*;
import java.util.*;

@Controller
public class SignatureBookController {
    private final SignatureBookService service;
    private final AccessControlService access;
    private final AuditService audit;
    private final SignatureBookPublicationService publication;
    private final com.bank.signaturemanagement.repository.SignatureBookEntryRepository entries;
    private final FileStorageService storage;

    public SignatureBookController(SignatureBookService service, AccessControlService access, AuditService audit, SignatureBookPublicationService publication, com.bank.signaturemanagement.repository.SignatureBookEntryRepository entries, FileStorageService storage) {
        this.service = service;
        this.access = access;
        this.audit = audit;
        this.publication = publication;
        this.entries = entries;
        this.storage = storage;
    }

    @GetMapping("/books")
    public String list(Authentication auth, Model model) {
        model.addAttribute("role", access.roleName(auth.getName()));
        model.addAttribute("books", service.visible(auth.getName()));
        model.addAttribute("canGenerate", access.has(auth.getName(), "BOOK_GENERATE"));
        model.addAttribute("currentYear", java.time.Year.now().getValue());
        return "books/index";
    }

    @PostMapping("/books/generate")
    public String generate(@RequestParam int year, @RequestParam String type, Authentication auth, RedirectAttributes redirect) {
        try {
            var b = service.generate(year, type, auth.getName());
            publication.publish(b, auth.getName());
            redirect.addFlashAttribute("success", "Generated " + b.getBookNumber());
            return "redirect:/books/" + b.getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/books";
        }
    }

    @GetMapping("/books/{id}")
    public String view(@PathVariable Long id, @RequestParam(defaultValue = "") String query, Authentication auth, Model model) {
        model.addAttribute("book", service.requireBook(id, auth.getName()));
        model.addAttribute("role", access.roleName(auth.getName()));
        model.addAttribute("canManage", access.has(auth.getName(), "BOOK_ACCESS_MANAGE"));
        model.addAttribute("canDownload", access.hasAnyRole(auth.getName(), "PD", "ADMIN"));
        model.addAttribute("query", query.trim());
        model.addAttribute("employees", groupEntries(entries.searchBookEntries(id, query.trim())));
        return "books/view";
    }

    @GetMapping("/books/{bookId}/entries/{entryId}/signature")
    public ResponseEntity<InputStreamResource> signature(@PathVariable Long bookId,@PathVariable Long entryId,Authentication auth,HttpServletRequest request) throws Exception {
        service.requireBook(bookId,auth.getName());
        var entry=entries.findById(entryId).orElseThrow(()->new IllegalArgumentException("Book signature not found"));
        if(entry.getBook()==null||!bookId.equals(entry.getBook().getId()))throw new org.springframework.security.access.AccessDeniedException("Signature is not part of this book");
        Path path=storage.resolveForRead(entry.getSignaturePath());
        if(!Files.isRegularFile(path))return ResponseEntity.notFound().build();
        String contentType=Files.probeContentType(path);
        audit.record(auth.getName(),"BOOK_SIGNATURE_VIEW","SIGNATURE_BOOK_ENTRY",entryId.toString(),request.getRemoteAddr(),"SUCCESS",null,null,entry.getSignatureType());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType==null?"image/png":contentType)).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"inline").contentLength(Files.size(path)).body(new InputStreamResource(Files.newInputStream(path)));
    }

    private List<EmployeeSignatureRow> groupEntries(List<com.bank.signaturemanagement.entity.SignatureBookEntry> source){Map<Long,MutableRow> grouped=new LinkedHashMap<>();for(var entry:source){var employee=entry.getEmployee();var row=grouped.computeIfAbsent(employee.getId(),key->new MutableRow(entry.getSerialNumber(),employee.getEmployeeNumber(),employee.getFullName(),employee.getDesignation()==null?"":employee.getDesignation().getDesignationName()));if("LOCAL".equals(entry.getSignatureType()))row.localId=entry.getId();if("FOREIGN".equals(entry.getSignatureType()))row.foreignId=entry.getId();}return grouped.values().stream().map(v->new EmployeeSignatureRow(v.serial,v.employeeNumber,v.name,v.designation,v.localId,v.foreignId)).toList();}
    public record EmployeeSignatureRow(int serial,String employeeNumber,String name,String designation,Long localId,Long foreignId){}
    private static class MutableRow{final int serial;final String employeeNumber,name,designation;Long localId,foreignId;MutableRow(int s,String e,String n,String d){serial=s;employeeNumber=e;name=n;designation=d;}}

    @GetMapping("/books/{id}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download, Authentication auth, HttpServletRequest request) throws Exception {
        if (download && !access.hasAnyRole(auth.getName(), "PD", "ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Only PD may download a signature book");
        }
        var b = service.requireBook(id, auth.getName());
        Path p = service.content(id, auth.getName());
        audit.record(auth.getName(), download ? "BOOK_DOWNLOAD" : "BOOK_VIEW", "SIGNATURE_BOOK", id.toString(), request.getRemoteAddr(), "SUCCESS", null, null, b.getBookNumber());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename=\"" + p.getFileName() + "\"").contentLength(Files.size(p)).body(new InputStreamResource(Files.newInputStream(p)));
    }
}
