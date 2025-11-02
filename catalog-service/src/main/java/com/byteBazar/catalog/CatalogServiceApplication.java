package com.byteBazar.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ByteBazar Catalog Service Main Application
 * 
 * Provides comprehensive product catalog management with:
 * - Full CRUD operations for products and categories
 * - Advanced search capabilities with JPA Specifications
 * - Redis caching for performance optimization
 * - OAuth2 JWT security integration
 * - RESTful APIs with OpenAPI documentation
 * - Database migrations with Flyway
 * - JPA auditing for created/updated timestamps
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableTransactionManagement
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
