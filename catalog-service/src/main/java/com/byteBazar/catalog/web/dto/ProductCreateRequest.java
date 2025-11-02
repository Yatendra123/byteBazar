package com.byteBazar.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating new products.
 */
@Schema(description = "Request to create a new product")
public record ProductCreateRequest(
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    @Schema(description = "Product name", example = "iPhone 15 Pro", required = true)
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    @Schema(description = "Short product description for listings")
    String shortDescription,
    
    @NotBlank(message = "Product SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    @Schema(description = "Product SKU", example = "IPH15PRO256GB", required = true)
    String sku,
    
    @NotBlank(message = "Product slug is required")
    @Size(max = 200, message = "Slug must not exceed 200 characters")
    @Schema(description = "URL-friendly product identifier", example = "iphone-15-pro-256gb", required = true)
    String slug,
    
    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Product price", example = "999.99", required = true)
    BigDecimal price,
    
    @DecimalMin(value = "0.0", message = "Compare price must be non-negative")
    @Schema(description = "Compare at price for discount display", example = "1099.99")
    BigDecimal comparePrice,
    
    @DecimalMin(value = "0.0", message = "Cost price must be non-negative")
    @Schema(description = "Internal cost price", example = "750.00")
    BigDecimal costPrice,
    
    @Schema(description = "Initial stock quantity", example = "50", defaultValue = "0")
    Integer stockQuantity,
    
    @Schema(description = "Low stock threshold", example = "5", defaultValue = "5")
    Integer lowStockThreshold,
    
    @Schema(description = "Whether to track inventory", example = "true", defaultValue = "true")
    Boolean trackInventory,
    
    @Schema(description = "Whether to allow backorders", example = "false", defaultValue = "false")
    Boolean allowBackorder,
    
    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    @Schema(description = "Product weight in kg", example = "0.240")
    BigDecimal weight,
    
    @DecimalMin(value = "0.0", message = "Length must be non-negative")
    @Schema(description = "Product length in cm", example = "15.9")
    BigDecimal dimensionsLength,
    
    @DecimalMin(value = "0.0", message = "Width must be non-negative")
    @Schema(description = "Product width in cm", example = "7.69")
    BigDecimal dimensionsWidth,
    
    @DecimalMin(value = "0.0", message = "Height must be non-negative")
    @Schema(description = "Product height in cm", example = "0.83")
    BigDecimal dimensionsHeight,
    
    @Schema(description = "Whether product is active", example = "true", defaultValue = "true")
    Boolean isActive,
    
    @Schema(description = "Whether product is featured", example = "false", defaultValue = "false")
    Boolean isFeatured,
    
    @Schema(description = "Whether product is digital", example = "false", defaultValue = "false")
    Boolean isDigital,
    
    @Size(max = 200, message = "Meta title must not exceed 200 characters")
    @Schema(description = "SEO meta title")
    String metaTitle,
    
    @Size(max = 500, message = "Meta description must not exceed 500 characters")
    @Schema(description = "SEO meta description")
    String metaDescription,
    
    @Size(max = 500, message = "Tags must not exceed 500 characters")
    @Schema(description = "Product tags (comma-separated)", example = "smartphone,apple,electronics")
    String tags,
    
    @NotNull(message = "Category ID is required")
    @Schema(description = "Category ID", required = true)
    UUID categoryId
) {
    
    /**
     * Apply default values for optional fields
     */
    public ProductCreateRequest withDefaults() {
        return new ProductCreateRequest(
            name,
            description,
            shortDescription,
            sku,
            slug,
            price,
            comparePrice,
            costPrice,
            stockQuantity != null ? stockQuantity : 0,
            lowStockThreshold != null ? lowStockThreshold : 5,
            trackInventory != null ? trackInventory : true,
            allowBackorder != null ? allowBackorder : false,
            weight,
            dimensionsLength,
            dimensionsWidth,
            dimensionsHeight,
            isActive != null ? isActive : true,
            isFeatured != null ? isFeatured : false,
            isDigital != null ? isDigital : false,
            metaTitle,
            metaDescription,
            tags,
            categoryId
        );
    }
}
