package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final FileStorageService fileStorageService;

    public WebConfig(FileStorageService fileStorageService) { this.fileStorageService = fileStorageService; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = fileStorageService.getUploadRoot().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(fileStorageService.getProfilePhotoRoot().toUri().toString());
        registry.addResourceHandler("/uploads/signature/**")
                .addResourceLocations(fileStorageService.getSignatureRoot().toUri().toString());
    }
}
