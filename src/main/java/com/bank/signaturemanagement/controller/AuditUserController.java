package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.entity.AuditLog;
import com.bank.signaturemanagement.service.AuditService;
import com.bank.signaturemanagement.service.AuditReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/audit")
public class AuditUserController {
    private final AuditService audit;
    private final AuditReportService reports;

    public AuditUserController(AuditService audit,AuditReportService reports) { this.audit = audit;this.reports=reports; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "audit/dashboard";
    }

    @GetMapping("/trail")
    public String trail(@RequestParam(defaultValue="") String query,
                        @RequestParam(defaultValue="") String action,
                        @RequestParam(defaultValue="0") int page,
                        Model model) {
        model.addAttribute("logs", reports.trail(query, action, page));
        model.addAttribute("query", query);
        model.addAttribute("action", action);
        model.addAttribute("auditRole", "AUDIT");
        model.addAttribute("reportAction", "/audit/report.csv");
        return "audit/trail";
    }

    @GetMapping("/reports/onboarding")
    public String onboarding(Model model){model.addAttribute("title","Employee onboarding report");model.addAttribute("reportType","onboarding");model.addAttribute("activePage","onboarding-report");model.addAttribute("rows",reports.onboarding());return "audit/report";}
    @GetMapping("/reports/signature-additions")
    public String signatureAdditions(Model model){model.addAttribute("title","Signature addition report");model.addAttribute("reportType","signature-additions");model.addAttribute("activePage","signature-additions-report");model.addAttribute("rows",reports.signatureAdditions());return "audit/report";}
    @GetMapping("/reports/signature-views")
    public String signatureViews(Model model){model.addAttribute("title","Signature view report");model.addAttribute("reportType","signature-views");model.addAttribute("activePage","signature-views-report");model.addAttribute("rows",reports.signatureViews());return "audit/report";}

    @GetMapping(value="/reports/{type}.csv",produces="text/csv")
    public void reportCsv(@org.springframework.web.bind.annotation.PathVariable String type,Authentication authentication,HttpServletRequest request,HttpServletResponse response)throws IOException{
        response.setHeader("Content-Disposition","attachment; filename="+type+"-report.csv");response.setHeader("Cache-Control","no-store");response.setCharacterEncoding("UTF-8");response.getWriter().write('\ufeff');
        try(CSVPrinter csv=new CSVPrinter(response.getWriter(),CSVFormat.DEFAULT)){
            switch(type){
                case "onboarding" -> {csv.printRecord("Employee ID","Employee number","Employee name","Onboarded at","Initiated by","Source","Batch number","DGM approved by","DGM approved at","GM approved by","GM approved at");for(var r:reports.onboarding())csv.printRecord(r.employeeId(),safe(r.employeeNumber()),safe(r.employeeName()),r.onboardedAt(),safe(r.initiatedBy()),r.source(),safe(r.batchNumber()),safe(r.dgmApprovedBy()),r.dgmApprovedAt(),safe(r.gmApprovedBy()),r.gmApprovedAt());}
                case "signature-additions" -> {csv.printRecord("Employee ID","Employee number","Employee name","Signature type","Added by","Added at","Request ID");for(var r:reports.signatureAdditions())csv.printRecord(r.employeeId(),safe(r.employeeNumber()),safe(r.employeeName()),r.signatureType(),safe(r.addedBy()),r.addedAt(),safe(r.requestNumber()));}
                case "signature-views" -> {csv.printRecord("Viewed by","Viewed at","Employee ID","Employee number","Employee name","Signature type","Result","IP address");for(var r:reports.signatureViews())csv.printRecord(safe(r.viewedBy()),r.viewedAt(),r.employeeId(),safe(r.employeeNumber()),safe(r.employeeName()),r.signatureType(),r.result(),safe(r.ipAddress()));}
                default -> throw new IllegalArgumentException("Unknown audit report");
            }
        }
        audit.record(authentication.getName(),"AUDIT_REPORT_EXPORT","AUDIT_REPORT",type,request.getRemoteAddr(),"SUCCESS",null,null,null);
    }

    @GetMapping(value="/report.csv", produces="text/csv")
    public void report(@RequestParam(defaultValue="") String query,
                       @RequestParam(defaultValue="") String action,
                       Authentication authentication, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=system-audit-report.csv");
        response.setHeader("Cache-Control", "no-store");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write('\ufeff');
        try (CSVPrinter csv = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT.builder()
                .setHeader("Time (UTC)","Username","Action","Entity","Target ID","IP address","Result","Old value","New value","Details","Correlation ID").get())) {
            int page=0;
            org.springframework.data.domain.Page<AuditLog> result;
            do {
                result=audit.search(query,action,page++,1000);
                for(AuditLog log:result.getContent()) csv.printRecord(
                        safe(log.getEventTime()),safe(log.getUsername()),safe(log.getActionType()),safe(log.getTargetEntity()),
                        safe(log.getTargetId()),safe(log.getIpAddress()),safe(log.getResult()),safe(log.getOldValue()),
                        safe(log.getNewValue()),safe(log.getDetails()),safe(log.getCorrelationId()));
            } while(result.hasNext());
        }
        audit.record(authentication.getName(),"AUDIT_REPORT_EXPORT","AUDIT_LOG","CSV",request.getRemoteAddr(),"SUCCESS",null,null,"query="+query+", action="+action);
    }

    private String safe(Object value) {
        String text=value==null?"":String.valueOf(value);
        return text.matches("^[=+@-].*") ? "'"+text : text;
    }
}
