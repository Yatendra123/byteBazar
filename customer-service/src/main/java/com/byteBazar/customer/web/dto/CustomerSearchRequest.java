package com.byteBazar.customer.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/**
 * Advanced search request DTO demonstrating complex query parameters.
 * This shows how to design APIs for sophisticated search functionality.
 */
public record CustomerSearchRequest(
        // Text search across name and email
        String query,
        
        // Date range filtering
        Instant createdAfter,
        Instant createdBefore,
        
        // Boolean filters
        Boolean hasPhone,
        
        // Domain-based filtering (for B2B scenarios)
        @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid domain format")
        String emailDomain,
        
        // Pagination parameters
        @Min(value = 0, message = "Page must be non-negative") 
        Integer page,
        
        @Min(value = 1, message = "Size must be positive")
        Integer size,
        
        // Sorting options
        String sortBy,
        String sortDirection
) {
    
    /**
     * Creates a search request with default pagination values.
     */
    public CustomerSearchRequest withDefaults() {
        return new CustomerSearchRequest(
            query,
            createdAfter,
            createdBefore,
            hasPhone,
            emailDomain,
            page != null ? page : 0,
            size != null ? Math.min(size, 100) : 10, // Cap at 100 for performance
            sortBy != null ? sortBy : "createdAt",
            sortDirection != null ? sortDirection : "desc"
        );
    }
}
