package com.byteBazar.catalog.web.dto;

import com.byteBazar.catalog.domain.ProductVariant;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for ProductVariant entities.
 */
@Schema(description = "Product variant response with pricing and inventory details")
public record ProductVariantResponse(
    @Schema(description = "Variant unique identifier")
    UUID id,
    
    @Schema(description = "Variant SKU", example = "IPH15PRO256GB-RED")
    String variantSku,
    
    @Schema(description = "Variant name", example = "Red - 256GB")
    String name,
    
    @Schema(description = "Display name combining all options", example = "Red - 256GB - Pro")
    String displayName,
    
    @Schema(description = "Variant identifier for frontend", example = "red-256gb-pro")
    String variantIdentifier,
    
    @Schema(description = "First option name", example = "Color")
    String option1Name,
    
    @Schema(description = "First option value", example = "Red")
    String option1Value,
    
    @Schema(description = "Second option name", example = "Storage")
    String option2Name,
    
    @Schema(description = "Second option value", example = "256GB")
    String option2Value,
    
    @Schema(description = "Third option name", example = "Model")
    String option3Name,
    
    @Schema(description = "Third option value", example = "Pro")
    String option3Value,
    
    @Schema(description = "Variant price (overrides product price if set)")
    BigDecimal price,
    
    @Schema(description = "Variant compare price")
    BigDecimal comparePrice,
    
    @Schema(description = "Effective price (variant or product price)")
    BigDecimal effectivePrice,
    
    @Schema(description = "Effective compare price")
    BigDecimal effectiveComparePrice,
    
    @Schema(description = "Whether variant is on sale")
    Boolean isOnSale,
    
    @Schema(description = "Discount percentage for this variant")
    BigDecimal discountPercentage,
    
    @Schema(description = "Variant stock quantity")
    Integer stockQuantity,
    
    @Schema(description = "Low stock threshold for variant")
    Integer lowStockThreshold,
    
    @Schema(description = "Variant weight in kg")
    BigDecimal weight,
    
    @Schema(description = "Variant barcode")
    String barcode,
    
    @Schema(description = "Whether variant is active")
    Boolean isActive,
    
    @Schema(description = "Whether this is the default variant")
    Boolean isDefault,
    
    @Schema(description = "Variant-specific image URL")
    String imageUrl,
    
    @Schema(description = "Position for ordering variants")
    Integer position,
    
    @Schema(description = "Whether variant is in stock")
    Boolean isInStock,
    
    @Schema(description = "Whether variant is low on stock")
    Boolean isLowStock,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Variant creation timestamp")
    Instant createdAt,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Variant last update timestamp")
    Instant updatedAt
) {
    
    public static ProductVariantResponse from(ProductVariant variant) {
        if (variant == null) return null;
        
        return new ProductVariantResponse(
            variant.getId(),
            variant.getVariantSku(),
            variant.getName(),
            variant.getDisplayName(),
            variant.getVariantIdentifier(),
            variant.getOption1Name(),
            variant.getOption1Value(),
            variant.getOption2Name(),
            variant.getOption2Value(),
            variant.getOption3Name(),
            variant.getOption3Value(),
            variant.getPrice(),
            variant.getComparePrice(),
            variant.getEffectivePrice(),
            variant.getEffectiveComparePrice(),
            variant.isOnSale(),
            variant.getDiscountPercentage(),
            variant.getStockQuantity(),
            variant.getLowStockThreshold(),
            variant.getWeight(),
            variant.getBarcode(),
            variant.getIsActive(),
            variant.getIsDefault(),
            variant.getImageUrl(),
            variant.getPosition(),
            variant.isInStock(),
            variant.isLowStock(),
            variant.getCreatedAt(),
            variant.getUpdatedAt()
        );
    }
    
    /**
     * Create simplified variant response for product listings
     */
    public static ProductVariantResponse simple(ProductVariant variant) {
        if (variant == null) return null;
        
        return new ProductVariantResponse(
            variant.getId(),
            variant.getVariantSku(),
            variant.getName(),
            variant.getDisplayName(),
            variant.getVariantIdentifier(),
            variant.getOption1Name(),
            variant.getOption1Value(),
            variant.getOption2Name(),
            variant.getOption2Value(),
            variant.getOption3Name(),
            variant.getOption3Value(),
            variant.getPrice(),
            variant.getComparePrice(),
            variant.getEffectivePrice(),
            variant.getEffectiveComparePrice(),
            variant.isOnSale(),
            variant.getDiscountPercentage(),
            variant.getStockQuantity(),
            null, // No low stock threshold for simple response
            null, // No weight for simple response
            null, // No barcode for simple response
            variant.getIsActive(),
            variant.getIsDefault(),
            variant.getImageUrl(),
            variant.getPosition(),
            variant.isInStock(),
            variant.isLowStock(),
            null, // No created timestamp for simple response
            null  // No updated timestamp for simple response
        );
    }
}
