package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.entity.Role;
import com.bank.signaturemanagement.entity.User;
import com.bank.signaturemanagement.repository.RoleRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration
public class InitialDataConfig {
    @Bean
    CommandLineRunner initializeRolesAndAdmin(RoleRepository roleRepository, UserRepository userRepository,
                                               PasswordEncoder encoder,
                                               @Value("${app.initial-admin-password}") String adminPassword,
                                               @Value("${app.initial-audit-password:123456789}") String auditPassword) {
        return args -> {
            Map<String, String> roles = Map.of(
                    "ADMIN", "Manages users", "PD", "Creates employee requests",
                    "DGM", "Level 1 approver", "GM", "Level 2 approver",
                    "BRANCH", "Views approved employees",
                    "AUDIT", "Read-only access to the complete audit ledger");
            roles.forEach((name, description) -> roleRepository.findByName(name)
                    .orElseGet(() -> roleRepository.save(new Role(name, description))));
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode(adminPassword));
                admin.setFullName("System Administrator");
                admin.setEmail("admin@bank.local");
                admin.setBranchId("SYSTEM");
                admin.setRole(roleRepository.findByName("ADMIN").orElseThrow());
                userRepository.save(admin);
            }
            if (!userRepository.existsByUsername("audit")) {
                User audit = new User();
                audit.setUsername("audit");
                audit.setPasswordHash(encoder.encode(auditPassword));
                audit.setFullName("Audit User");
                audit.setEmail("audit@bank.local");
                audit.setBranchId("SYSTEM");
                audit.setRole(roleRepository.findByName("AUDIT").orElseThrow());
                audit.setSignatureScope("BOTH");
                audit.setApprovalStatus("APPROVED");
                audit.setMustChangePassword(false);
                userRepository.save(audit);
            }
        };
    }
}
