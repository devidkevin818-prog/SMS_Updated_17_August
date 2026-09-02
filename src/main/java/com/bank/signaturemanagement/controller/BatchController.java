package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.BatchImportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/pd/batches", "/admin/batches"})
@org.springframework.security.access.prepost.PreAuthorize("@accessControl.has(authentication.name,'BATCH_UPLOAD')")
public class BatchController {
    private final BatchImportService service;

    public BatchController(BatchImportService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model, jakarta.servlet.http.HttpServletRequest request) {
        boolean admin = request.getRequestURI().startsWith("/admin");
        model.addAttribute("batches", service.all());
        model.addAttribute("batchBase", admin ? "/admin/batches" : "/pd/batches");
        model.addAttribute("pageRole", admin ? "ADMIN" : "PD");
        return "batches/index";
    }

    @PostMapping
    public String upload(@RequestParam MultipartFile file, @RequestParam(required = false) Long retryOfId, Authentication auth, RedirectAttributes redirect, jakarta.servlet.http.HttpServletRequest request) {
        try {
            var b = service.upload(file, retryOfId, auth.getName());
            redirect.addFlashAttribute("success", "Batch " + b.getBatchNumber() + " uploaded. Review and correct the rows before submission.");
            return "redirect:" + (request.getRequestURI().startsWith("/admin") ? "/admin/batches/" : "/pd/batches/") + b.getId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + (request.getRequestURI().startsWith("/admin") ? "/admin/batches" : "/pd/batches");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, jakarta.servlet.http.HttpServletRequest request) {
        boolean admin = request.getRequestURI().startsWith("/admin");
        model.addAttribute("batch", service.get(id));
        model.addAttribute("items", service.itemViews(id));
        model.addAttribute("batchBase", admin ? "/admin/batches" : "/pd/batches");
        model.addAttribute("pageRole", admin ? "ADMIN" : "PD");
        model.addAttribute("batchReadOnly", false);
        return "batches/detail";
    }

    @PostMapping("/{batchId}/rows/{itemId}")
    public String updateRow(@PathVariable Long batchId,@PathVariable Long itemId,@RequestParam String employeeId,@RequestParam String name,@RequestParam String designation,@RequestParam String department,@RequestParam String branch,@RequestParam String status,@RequestParam String classification,@RequestParam String joiningDate,Authentication auth,RedirectAttributes redirect,jakarta.servlet.http.HttpServletRequest request){
        String base=request.getRequestURI().startsWith("/admin")?"/admin/batches":"/pd/batches";
        try{service.updateRow(batchId,itemId,new BatchImportService.BatchRow(employeeId,name,designation,department,branch,status,classification,joiningDate),auth.getName());redirect.addFlashAttribute("success","Row updated and revalidated");}
        catch(IllegalArgumentException|IllegalStateException e){redirect.addFlashAttribute("error",e.getMessage());}
        return "redirect:"+base+"/"+batchId;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,Authentication auth,RedirectAttributes redirect,jakarta.servlet.http.HttpServletRequest request){
        String base=request.getRequestURI().startsWith("/admin")?"/admin/batches":"/pd/batches";
        try{service.submit(id,auth.getName());redirect.addFlashAttribute("success","Batch submitted for DGM approval");}
        catch(IllegalArgumentException|IllegalStateException e){redirect.addFlashAttribute("error",e.getMessage());}
        return "redirect:"+base+"/"+id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,Authentication auth,RedirectAttributes redirect,jakarta.servlet.http.HttpServletRequest request){String base=request.getRequestURI().startsWith("/admin")?"/admin/batches":"/pd/batches";try{service.cancel(id,auth.getName());redirect.addFlashAttribute("success","Batch cancelled and retained for audit");}catch(IllegalArgumentException|IllegalStateException e){redirect.addFlashAttribute("error",e.getMessage());}return "redirect:"+base;}
}
