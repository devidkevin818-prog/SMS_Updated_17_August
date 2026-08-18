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
                                               @Value("${app.initial-admin-password}") String adminPassword) {
        return args -> {
            Map<String, String> roles = Map.of(
                    "ADMIN", "Manages users", "PD", "Creates employee requests",
                    "DGM", "Level 1 approver", "GM", "Level 2 approver",
                    "BRANCH", "Views approved employees");
            roles.forEach((name, description) -> roleRepository.findByName(name)
                    .orElseGet(() -> roleRepository.save(new Role(name, description))));
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(encoder.encode(adminPassword));
                admin.setFullName("System Administrator");
                admin.setEmail("admin@bank.local");
                admin.setRole(roleRepository.findByName("ADMIN").orElseThrow());
                userRepository.save(admin);
            }
        };
    }
}
