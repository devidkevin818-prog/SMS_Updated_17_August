package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.security.RoleLoginSuccessHandler;
import com.bank.signaturemanagement.security.AuditedLoginFailureHandler;
import com.bank.signaturemanagement.security.AuditedLogoutSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RoleLoginSuccessHandler successHandler,
                                                   AuditedLoginFailureHandler failureHandler,
                                                   AuditedLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/pd/**").hasAnyRole("PD", "ADMIN")
                        .requestMatchers("/dgm/**").hasAnyRole("DGM", "ADMIN")
                        .requestMatchers("/gm/**").hasAnyRole("GM", "ADMIN")
                        .requestMatchers("/branch/**").hasAnyRole("BRANCH", "ADMIN")
                        .requestMatchers("/media/**", "/books/**").authenticated()
                        .requestMatchers("/uploads/**").denyAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler).permitAll());
        return http.build();
    }
}
