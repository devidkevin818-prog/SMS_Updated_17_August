package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.service.FileStorageService;
import com.bank.signaturemanagement.security.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final FileStorageService fileStorageService;
    private final AuditInterceptor auditInterceptor;

    public WebConfig(FileStorageService fileStorageService, AuditInterceptor auditInterceptor) {
        this.fileStorageService = fileStorageService;
        this.auditInterceptor = auditInterceptor;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getUploadRoot().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(fileStorageService.getProfilePhotoRoot().toUri().toString());
        registry.addResourceHandler("/uploads/signature/**")
                .addResourceLocations(fileStorageService.getSignatureRoot().toUri().toString());
        registry.addResourceHandler("/uploads/foreign-signature/**")
                .addResourceLocations(
                        fileStorageService
                                .getForeignSignatureRoot()
                                .toUri()
                                .toString()
                );
    }

}
