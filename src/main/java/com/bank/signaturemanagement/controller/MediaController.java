package com.bank.signaturemanagement.controller;

import com.bank.signaturemanagement.service.AccessControlService;
import com.bank.signaturemanagement.service.AuditService;
import com.bank.signaturemanagement.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class MediaController {
    private final FileStorageService storage;
    private final AccessControlService access;
    private final AuditService audit;

    public MediaController(FileStorageService storage, AccessControlService access, AuditService audit) {
        this.storage=storage; this.access=access; this.audit=audit;
    }

    @GetMapping("/media/{*storedPath}")
    public ResponseEntity<InputStreamResource> read(@PathVariable String storedPath, Authentication authentication,
                                                     HttpServletRequest request) throws Exception {
        String normalized=storedPath.startsWith("/")?storedPath.substring(1):storedPath;
        MediaKind kind=mediaKind(normalized);
        if (kind == null || !access.has(authentication.getName(), "EMPLOYEE_VIEW")
                || (kind.pending && !access.hasAnyRole(authentication.getName(), "ADMIN", "PD", "DGM", "GM"))
                || (kind.signatureType != null && !access.canViewSignature(authentication.getName(),kind.signatureType))) {
            audit.record(authentication.getName(),"MEDIA_VIEW","MEDIA",normalized,request.getRemoteAddr(),"DENIED",null,null,kind==null?"unapproved path":kind.signatureType);
            throw new AccessDeniedException("Your signature classification does not permit this file");
        }
        Path path=storage.resolveForRead(normalized);
        if (!Files.isRegularFile(path)) return ResponseEntity.notFound().build();
        String contentType=Files.probeContentType(path);
        audit.record(authentication.getName(),"MEDIA_VIEW",kind.signatureType==null?"PHOTO":"SIGNATURE",normalized,request.getRemoteAddr(),"SUCCESS",null,null,kind.signatureType);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType==null?MediaType.APPLICATION_OCTET_STREAM_VALUE:contentType))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+path.getFileName()+"\"")
                .contentLength(Files.size(path)).body(new InputStreamResource(Files.newInputStream(path)));
    }

    private MediaKind mediaKind(String path) {
        if (path.startsWith("foreign-signature/")) return new MediaKind("FOREIGN",false);
        if (path.startsWith("pending-foreign-signature/")) return new MediaKind("FOREIGN",true);
        if (path.startsWith("signature/")) return new MediaKind("LOCAL",false);
        if (path.startsWith("employee-signature/") || path.startsWith("pending-signature/") || path.startsWith("pending-local-signature/")) return new MediaKind("LOCAL",true);
        if (path.startsWith("profile/")) return new MediaKind(null,false);
        if (path.startsWith("employee-photo/") || path.startsWith("pending-photo/")) return new MediaKind(null,true);
        return null;
    }

    private record MediaKind(String signatureType, boolean pending) {}
}
