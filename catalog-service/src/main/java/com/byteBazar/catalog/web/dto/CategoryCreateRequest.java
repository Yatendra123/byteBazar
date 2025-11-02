package com.byteBazar.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating new categories.
 */
@Schema(description = "Request to create a new category")
public record CategoryCreateRequest(
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category name", example = "Electronics", required = true)
    String name,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    String description,
    
    @NotBlank(message = "Category slug is required")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Schema(description = "URL-friendly category identifier", example = "electronics", required = true)
    String slug,
    
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Schema(description = "Category image URL", example = "https://example.com/images/electronics.jpg")
    String imageUrl,
    
    @Schema(description = "Parent category ID if this is a subcategory")
    UUID parentId,
    
    @Schema(description = "Display order for sorting", example = "1")
    Integer displayOrder,
    
    @Schema(description = "Whether the category is active", example = "true", defaultValue = "true")
    Boolean isActive
) {
    
    /**
     * Apply default values for optional fields
     */
    public CategoryCreateRequest withDefaults() {
        return new CategoryCreateRequest(
            name,
            description,
            slug,
            imageUrl,
            parentId,
            displayOrder != null ? displayOrder : 0,
            isActive != null ? isActive : true
        );
    }
}
