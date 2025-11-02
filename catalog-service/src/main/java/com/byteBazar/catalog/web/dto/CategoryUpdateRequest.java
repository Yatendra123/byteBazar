package com.byteBazar.catalog.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating categories.
 * Uses optional fields for partial updates.
 */
@Schema(description = "Request to update an existing category")
public record CategoryUpdateRequest(
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category name", example = "Electronics")
    String name,
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    String description,
    
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Schema(description = "URL-friendly category identifier", example = "electronics")
    String slug,
    
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Schema(description = "Category image URL", example = "https://example.com/images/electronics.jpg")
    String imageUrl,
    
    @Schema(description = "Parent category ID if this is a subcategory")
    UUID parentId,
    
    @Schema(description = "Display order for sorting", example = "1")
    Integer displayOrder,
    
    @Schema(description = "Whether the category is active", example = "true")
    Boolean isActive
) {}
