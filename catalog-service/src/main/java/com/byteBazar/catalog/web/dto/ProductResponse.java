package com.byteBazar.catalog.web.dto;

import com.byteBazar.catalog.domain.Product;
import com.byteBazar.catalog.domain.ProductImage;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for Product entities.
 * Provides comprehensive product information with inventory and pricing details.
 */
@Schema(description = "Product response containing complete product details")
public record ProductResponse(
    @Schema(description = "Product unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,
    
    @Schema(description = "Product name", example = "iPhone 15 Pro")
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @Schema(description = "Short product description for listings")
    String shortDescription,
    
    @Schema(description = "Product SKU", example = "IPH15PRO256GB")
    String sku,
    
    @Schema(description = "URL-friendly product identifier", example = "iphone-15-pro-256gb")
    String slug,
    
    @Schema(description = "Product price", example = "999.99")
    BigDecimal price,
    
    @Schema(description = "Compare at price for discount display", example = "1099.99")
    BigDecimal comparePrice,
    
    @Schema(description = "Discount percentage if on sale", example = "9.09")
    BigDecimal discountPercentage,
    
    @Schema(description = "Whether product is on sale")
    Boolean isOnSale,
    
    @Schema(description = "Current stock quantity", example = "50")
    Integer stockQuantity,
    
    @Schema(description = "Low stock threshold", example = "5")
    Integer lowStockThreshold,
    
    @Schema(description = "Whether inventory is tracked")
    Boolean trackInventory,
    
    @Schema(description = "Whether backorders are allowed")
    Boolean allowBackorder,
    
    @Schema(description = "Product availability status")
    String stockStatus,
    
    @Schema(description = "Whether product is in stock")
    Boolean isInStock,
    
    @Schema(description = "Whether product is low on stock")
    Boolean isLowStock,
    
    @Schema(description = "Product weight in kg", example = "0.240")
    BigDecimal weight,
    
    @Schema(description = "Product dimensions - length in cm", example = "15.9")
    BigDecimal dimensionsLength,
    
    @Schema(description = "Product dimensions - width in cm", example = "7.69")
    BigDecimal dimensionsWidth,
    
    @Schema(description = "Product dimensions - height in cm", example = "0.83")
    BigDecimal dimensionsHeight,
    
    @Schema(description = "Whether product is active")
    Boolean isActive,
    
    @Schema(description = "Whether product is featured")
    Boolean isFeatured,
    
    @Schema(description = "Whether product is digital")
    Boolean isDigital,
    
    @Schema(description = "SEO meta title")
    String metaTitle,
    
    @Schema(description = "SEO meta description")
    String metaDescription,
    
    @Schema(description = "Product tags")
    String tags,
    
    @Schema(description = "Category information")
    CategoryResponse category,
    
    @Schema(description = "Product images")
    List<ProductImageResponse> images,
    
    @Schema(description = "Primary product image")
    ProductImageResponse primaryImage,
    
    @Schema(description = "Product variants")
    List<ProductVariantResponse> variants,
    
    @Schema(description = "Whether product has variants")
    Boolean hasVariants,
    
    @Schema(description = "Total inventory including variants")
    Integer totalInventory,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Product creation timestamp")
    Instant createdAt,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Product last update timestamp")
    Instant updatedAt
) {
    
    /**
     * Create ProductResponse from Product entity
     */
    public static ProductResponse from(Product product) {
        return from(product, true, true, true);
    }
    
    /**
     * Create ProductResponse with options for including related data
     */
    public static ProductResponse from(Product product, boolean includeCategory, boolean includeImages, boolean includeVariants) {
        if (product == null) return null;
        
        CategoryResponse categoryResponse = null;
        if (includeCategory && product.getCategory() != null) {
            categoryResponse = CategoryResponse.simple(product.getCategory());
        }
        
        List<ProductImageResponse> imageResponses = null;
        ProductImageResponse primaryImageResponse = null;
        if (includeImages && product.getImages() != null) {
            imageResponses = product.getImages().stream()
                                   .map(ProductImageResponse::from)
                                   .collect(Collectors.toList());
            
            ProductImage primaryImage = product.getPrimaryImage();
            if (primaryImage != null) {
                primaryImageResponse = ProductImageResponse.from(primaryImage);
            }
        }
        
        List<ProductVariantResponse> variantResponses = null;
        if (includeVariants && product.getVariants() != null) {
            variantResponses = product.getVariants().stream()
                                     .filter(variant -> variant.getIsActive())
                                     .map(ProductVariantResponse::from)
                                     .collect(Collectors.toList());
        }
        
        String stockStatus = determineStockStatus(product);
        
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getShortDescription(),
            product.getSku(),
            product.getSlug(),
            product.getPrice(),
            product.getComparePrice(),
            product.getDiscountPercentage(),
            product.isOnSale(),
            product.getStockQuantity(),
            product.getLowStockThreshold(),
            product.getTrackInventory(),
            product.getAllowBackorder(),
            stockStatus,
            product.isInStock(),
            product.isLowStock(),
            product.getWeight(),
            product.getDimensionsLength(),
            product.getDimensionsWidth(),
            product.getDimensionsHeight(),
            product.getIsActive(),
            product.getIsFeatured(),
            product.getIsDigital(),
            product.getMetaTitle(),
            product.getMetaDescription(),
            product.getTags(),
            categoryResponse,
            imageResponses,
            primaryImageResponse,
            variantResponses,
            product.hasVariants(),
            product.getTotalInventory(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
    
    /**
     * Create simplified product response for listings
     */
    public static ProductResponse simple(Product product) {
        if (product == null) return null;
        
        ProductImage primaryImage = product.getPrimaryImage();
        ProductImageResponse primaryImageResponse = null;
        if (primaryImage != null) {
            primaryImageResponse = ProductImageResponse.from(primaryImage);
        }
        
        String stockStatus = determineStockStatus(product);
        
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getShortDescription(),
            null, // No full description for simple response
            product.getSku(),
            product.getSlug(),
            product.getPrice(),
            product.getComparePrice(),
            product.getDiscountPercentage(),
            product.isOnSale(),
            product.getStockQuantity(),
            product.getLowStockThreshold(),
            product.getTrackInventory(),
            product.getAllowBackorder(),
            stockStatus,
            product.isInStock(),
            product.isLowStock(),
            product.getWeight(),
            null, null, null, // No dimensions for simple response
            product.getIsActive(),
            product.getIsFeatured(),
            product.getIsDigital(),
            null, null, // No SEO fields for simple response
            product.getTags(),
            CategoryResponse.simple(product.getCategory()),
            null, // No images list for simple response
            primaryImageResponse,
            null, // No variants for simple response
            product.hasVariants(),
            product.getTotalInventory(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
    
    /**
     * Create product response for search results
     */
    public static ProductResponse forSearch(Product product) {
        return simple(product);
    }
    
    /**
     * Create detailed product response with all relations
     */
    public static ProductResponse detailed(Product product) {
        return from(product, true, true, true);
    }
    
    /**
     * Determine stock status text
     */
    private static String determineStockStatus(Product product) {
        if (!product.getTrackInventory()) {
            return "IN_STOCK";
        }
        
        if (product.isOutOfStock()) {
            return product.getAllowBackorder() ? "BACKORDER" : "OUT_OF_STOCK";
        }
        
        if (product.isLowStock()) {
            return "LOW_STOCK";
        }
        
        return "IN_STOCK";
    }
}
