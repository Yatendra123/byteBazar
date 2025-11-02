package com.byteBazar.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for advanced product search with filtering.
 * Following the same pattern as CustomerSearchRequest with defaults.
 */
@Schema(description = "Advanced product search request with multiple filter options")
public record ProductSearchRequest(
    @Schema(description = "Search query text", example = "iPhone")
    String query,
    
    @Schema(description = "Category ID to filter by")
    UUID categoryId,
    
    @Schema(description = "Minimum price filter", example = "100.00")
    BigDecimal minPrice,
    
    @Schema(description = "Maximum price filter", example = "2000.00")
    BigDecimal maxPrice,
    
    @Schema(description = "Filter by featured products only")
    Boolean isFeatured,
    
    @Schema(description = "Filter by in-stock products only")
    Boolean isInStock,
    
    @Schema(description = "Filter by products on sale only")
    Boolean isOnSale,
    
    @Schema(description = "Filter by digital products")
    Boolean isDigital,
    
    @Schema(description = "Tags to filter by")
    List<String> tags,
    
    @Schema(description = "Minimum weight filter in kg")
    BigDecimal minWeight,
    
    @Schema(description = "Maximum weight filter in kg")
    BigDecimal maxWeight,
    
    @Schema(description = "Filter by products created after this date")
    Instant createdAfter,
    
    @Schema(description = "Filter by products created before this date")
    Instant createdBefore,
    
    @Schema(description = "Field to sort by", example = "name", allowableValues = {"name", "price", "createdAt", "updatedAt", "stockQuantity"})
    String sortBy,
    
    @Schema(description = "Sort direction", example = "asc", allowableValues = {"asc", "desc"})
    String sortDirection,
    
    @Min(value = 0, message = "Page number must be non-negative")
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    Integer page,
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    @Schema(description = "Page size", example = "20", defaultValue = "20")
    Integer size
) {
    
    /**
     * Apply default values for pagination and sorting
     */
    public ProductSearchRequest withDefaults() {
        return new ProductSearchRequest(
            query,
            categoryId,
            minPrice,
            maxPrice,
            isFeatured,
            isInStock,
            isOnSale,
            isDigital,
            tags,
            minWeight,
            maxWeight,
            createdAfter,
            createdBefore,
            sortBy != null ? sortBy : "createdAt",
            sortDirection != null ? sortDirection : "desc",
            page != null ? page : 0,
            size != null ? size : 20
        );
    }
    
    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return query != null && !query.trim().isEmpty() ||
               categoryId != null ||
               minPrice != null ||
               maxPrice != null ||
               isFeatured != null ||
               isInStock != null ||
               isOnSale != null ||
               isDigital != null ||
               tags != null && !tags.isEmpty() ||
               minWeight != null ||
               maxWeight != null ||
               createdAfter != null ||
               createdBefore != null;
    }
    
    /**
     * Check if price range filter is applied
     */
    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }
    
    /**
     * Check if weight range filter is applied
     */
    public boolean hasWeightRange() {
        return minWeight != null || maxWeight != null;
    }
    
    /**
     * Check if date range filter is applied
     */
    public boolean hasDateRange() {
        return createdAfter != null || createdBefore != null;
    }
}
