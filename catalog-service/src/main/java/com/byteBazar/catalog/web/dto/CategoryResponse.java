package com.byteBazar.catalog.web.dto;

import com.byteBazar.catalog.domain.Category;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Category entities.
 * Provides clean API representation with optional hierarchical data.
 */
@Schema(description = "Category response containing category details and hierarchy information")
public record CategoryResponse(
    @Schema(description = "Category unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Category name", example = "Electronics")
    String name,
    
    @Schema(description = "Category description", example = "Electronic devices and accessories")
    String description,
    
    @Schema(description = "URL-friendly category identifier", example = "electronics")
    String slug,
    
    @Schema(description = "Category image URL", example = "https://example.com/images/electronics.jpg")
    String imageUrl,
    
    @Schema(description = "Whether the category is active", example = "true")
    Boolean isActive,
    
    @Schema(description = "Display order for sorting", example = "1")
    Integer displayOrder,
    
    @Schema(description = "Parent category ID if this is a subcategory")
    UUID parentId,
    
    @Schema(description = "Parent category name if this is a subcategory")
    String parentName,
    
    @Schema(description = "Full category path", example = "Electronics > Smartphones")
    String fullPath,
    
    @Schema(description = "Number of products in this category")
    Long productCount,
    
    @Schema(description = "Number of child categories")
    Integer childrenCount,
    
    @Schema(description = "Whether this category has child categories")
    Boolean hasChildren,
    
    @Schema(description = "Child categories (if requested)")
    List<CategoryResponse> children,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Category creation timestamp")
    Instant createdAt,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Category last update timestamp")
    Instant updatedAt
) {
    
    /**
     * Create CategoryResponse from Category entity
     */
    public static CategoryResponse from(Category category) {
        return from(category, false, false);
    }
    
    /**
     * Create CategoryResponse from Category entity with options
     */
    public static CategoryResponse from(Category category, boolean includeChildren, boolean includeProductCount) {
        if (category == null) return null;
        
        List<CategoryResponse> children = null;
        if (includeChildren && category.hasChildren()) {
            children = category.getChildren().stream()
                             .filter(child -> child.getIsActive())
                             .map(child -> CategoryResponse.from(child, false, includeProductCount))
                             .collect(Collectors.toList());
        }
        
        Long productCount = null;
        if (includeProductCount) {
            productCount = category.getProducts().stream()
                                 .filter(product -> product.getIsActive())
                                 .count();
        }
        
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getSlug(),
            category.getImageUrl(),
            category.getIsActive(),
            category.getDisplayOrder(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getParent() != null ? category.getParent().getName() : null,
            category.getFullPath(),
            productCount,
            category.getChildren() != null ? category.getChildren().size() : 0,
            category.hasChildren(),
            children,
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
    
    /**
     * Create simplified category response for listings
     */
    public static CategoryResponse simple(Category category) {
        if (category == null) return null;
        
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getSlug(),
            category.getImageUrl(),
            category.getIsActive(),
            category.getDisplayOrder(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getParent() != null ? category.getParent().getName() : null,
            category.getFullPath(),
            null, // No product count for simple response
            null, // No children count for simple response
            category.hasChildren(),
            null, // No children for simple response
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
    
    /**
     * Create category response with full hierarchy
     */
    public static CategoryResponse withFullHierarchy(Category category) {
        return from(category, true, true);
    }
}
