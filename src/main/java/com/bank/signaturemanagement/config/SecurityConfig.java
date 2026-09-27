package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.security.AuditedLoginFailureHandler;
import com.bank.signaturemanagement.security.AuditedLogoutSuccessHandler;
import com.bank.signaturemanagement.security.RoleLoginSuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RoleLoginSuccessHandler successHandler,
            AuditedLoginFailureHandler failureHandler,
            AuditedLogoutSuccessHandler logoutSuccessHandler
    ) throws Exception {

        http
                .authorizeHttpRequests(requests -> requests

                        /*
                         * Public resources
                         */
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        /*
                         * Signature book generation.
                         *
                         * Put this specific POST rule before the general
                         * /books/** GET rule.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/books/generate"
                        ).hasAnyRole(
                                "ADMIN",
                                "MAKER"
                        )

                        /*
                         * Signature book list and PDF viewing.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/books",
                                "/books/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "MAKER",
                                "LEVEL_1_CHECKER",
                                "LEVEL_2_CHECKER",
                                "BRANCH",
                                "AUDIT"
                        )

                        /*
                         * Media viewing.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/media/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "MAKER",
                                "LEVEL_1_CHECKER",
                                "LEVEL_2_CHECKER",
                                "BRANCH",
                                "AUDIT"
                        )

                        /*
                         * Administrator endpoints
                         */
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        /*
                         * Maker endpoints
                         */
                        .requestMatchers("/pd/**")
                        .hasAnyRole(
                                "MAKER",
                                "ADMIN"
                        )

                        /*
                         * Level 1 Checker endpoints
                         */
                        .requestMatchers("/dgm/**")
                        .hasAnyRole(
                                "LEVEL_1_CHECKER",
                                "ADMIN"
                        )

                        /*
                         * Level 2 Checker endpoints
                         */
                        .requestMatchers("/gm/**")
                        .hasAnyRole(
                                "LEVEL_2_CHECKER",
                                "ADMIN"
                        )

                        /*
                         * Branch endpoints
                         */
                        .requestMatchers("/branch/**")
                        .hasAnyRole(
                                "BRANCH",
                                "ADMIN"
                        )

                        /*
                         * Audit endpoints
                         */
                        .requestMatchers(
                                "/audit/dashboard",
                                "/audit/trail",
                                "/audit/report.csv",
                                "/audit/reports/**"
                        ).hasAnyRole(
                                "AUDIT",
                                "ADMIN"
                        )

                        /*
                         * Never expose uploaded files directly.
                         * Files should be served through an authorized
                         * controller such as /media/**.
                         */
                        .requestMatchers("/uploads/**")
                        .denyAll()

                        /*
                         * Every other request requires authentication.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * Keep CSRF protection enabled.
                 * POST forms must submit the CSRF token.
                 */
                .csrf(csrf -> {
                    // Default CSRF configuration is intentionally retained.
                })

                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .permitAll()
                );

        return http.build();
    }
}