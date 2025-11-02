package com.byteBazar.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").hasAuthority("SCOPE_payment.read")
                    .requestMatchers(HttpMethod.POST, "/api/v1/payments/**").hasAuthority("SCOPE_payment.charge")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/payments/*/refund").hasAuthority("SCOPE_payment.charge")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
