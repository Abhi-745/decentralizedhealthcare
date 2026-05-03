package com.example.VeristasId.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final OpaSecurityFilter opaSecurityFilter;

    public SecurityConfig(OpaSecurityFilter opaSecurityFilter) {
        this.opaSecurityFilter = opaSecurityFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for APIs
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // For H2 Console
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // Let the Filter handle the logic
                )
                .addFilterBefore(opaSecurityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
