package com.byteBazar.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for Catalog Service
 * Configures OAuth2 Resource Server with JWT tokens
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Public read access to products and categories
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // Write operations require authentication
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyAuthority("ADMIN", "CATALOG_MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyAuthority("ADMIN", "CATALOG_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAuthority("ADMIN")
                
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyAuthority("ADMIN", "CATALOG_MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyAuthority("ADMIN", "CATALOG_MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ADMIN")
                
                // Inventory management
                .requestMatchers("/api/products/*/inventory/**").hasAnyAuthority("ADMIN", "CATALOG_MANAGER", "INVENTORY_MANAGER")
                
                // Analytics endpoints require specific roles
                .requestMatchers("/api/analytics/**").hasAnyAuthority("ADMIN", "ANALYST", "CATALOG_MANAGER")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix(""); // Remove default SCOPE_ prefix
        authoritiesConverter.setAuthoritiesClaimName("authorities"); // Use 'authorities' claim

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        converter.setPrincipalClaimName("sub"); // Use 'sub' claim for principal name
        
        return converter;
    }
}
