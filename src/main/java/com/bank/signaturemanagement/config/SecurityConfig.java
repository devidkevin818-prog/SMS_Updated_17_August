package com.bank.signaturemanagement.config;

import com.bank.signaturemanagement.security.RoleLoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RoleLoginSuccessHandler successHandler) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/pd/**").hasAnyRole("PD", "ADMIN")
                        .requestMatchers("/dgm/**").hasAnyRole("DGM", "ADMIN")
                        .requestMatchers("/gm/**").hasAnyRole("GM", "ADMIN")
                        .requestMatchers("/branch/**").hasAnyRole("BRANCH", "ADMIN")
                        .requestMatchers("/uploads/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
