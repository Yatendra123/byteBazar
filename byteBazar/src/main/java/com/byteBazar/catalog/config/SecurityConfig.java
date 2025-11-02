package com.byteBazar.catalog.config;

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
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/public/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/products/**").hasAuthority("SCOPE_catalog.read")
                    .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAuthority("SCOPE_catalog.write")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAuthority("SCOPE_catalog.write")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAuthority("SCOPE_catalog.write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
