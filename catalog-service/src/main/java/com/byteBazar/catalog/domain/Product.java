package com.byteBazar.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Product entity representing items in the catalog.
 * Includes inventory tracking, pricing, and rich metadata.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_sku", columnList = "sku"),
    @Index(name = "idx_product_active", columnList = "is_active"),
    @Index(name = "idx_product_price", columnList = "price")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    // Pricing
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 10, scale = 2)
    private BigDecimal comparePrice; // Original price for discount display

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice; // Internal cost for profit calculation

    // Inventory
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = true;

    @Column(name = "allow_backorder", nullable = false)
    private Boolean allowBackorder = false;

    // Physical attributes
    @Column(name = "weight", precision = 8, scale = 3)
    private BigDecimal weight; // in kg

    @Column(name = "dimensions_length", precision = 8, scale = 2)
    private BigDecimal dimensionsLength; // in cm

    @Column(name = "dimensions_width", precision = 8, scale = 2)
    private BigDecimal dimensionsWidth; // in cm

    @Column(name = "dimensions_height", precision = 8, scale = 2)
    private BigDecimal dimensionsHeight; // in cm

    // Status and visibility
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "is_digital", nullable = false)
    private Boolean isDigital = false;

    // SEO and marketing
    @Column(name = "meta_title", length = 200)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated tags

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (isFeatured == null) {
            isFeatured = false;
        }
        if (isDigital == null) {
            isDigital = false;
        }
        if (trackInventory == null) {
            trackInventory = true;
        }
        if (allowBackorder == null) {
            allowBackorder = false;
        }
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (lowStockThreshold == null) {
            lowStockThreshold = 5;
        }
    }

    // Business logic methods

    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        if (!trackInventory) {
            return true; // If not tracking inventory, assume always in stock
        }
        return stockQuantity > 0 || allowBackorder;
    }

    /**
     * Check if product is low on stock
     */
    public boolean isLowStock() {
        if (!trackInventory) {
            return false;
        }
        return stockQuantity <= lowStockThreshold && stockQuantity > 0;
    }

    /**
     * Check if product is out of stock
     */
    public boolean isOutOfStock() {
        if (!trackInventory) {
            return false;
        }
        return stockQuantity <= 0;
    }

    /**
     * Calculate discount percentage if compare price exists
     */
    public BigDecimal getDiscountPercentage() {
        if (comparePrice == null || comparePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (price.compareTo(comparePrice) >= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = comparePrice.subtract(price);
        return discount.divide(comparePrice, 4, BigDecimal.ROUND_HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if product is on sale (has compare price higher than current price)
     */
    public boolean isOnSale() {
        return comparePrice != null && 
               comparePrice.compareTo(BigDecimal.ZERO) > 0 && 
               price.compareTo(comparePrice) < 0;
    }

    /**
     * Get the main/primary image of the product
     */
    public ProductImage getPrimaryImage() {
        return images.stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(images.isEmpty() ? null : images.get(0));
    }

    /**
     * Check if product has variants
     */
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    /**
     * Get total inventory including all variants
     */
    public Integer getTotalInventory() {
        if (!hasVariants()) {
            return stockQuantity;
        }
        return variants.stream()
                      .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                      .sum();
    }

    /**
     * Calculate profit margin percentage
     */
    public BigDecimal getProfitMargin() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal profit = price.subtract(costPrice);
        return profit.divide(price, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
    }
}
