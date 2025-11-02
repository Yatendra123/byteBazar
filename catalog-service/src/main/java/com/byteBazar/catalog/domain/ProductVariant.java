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
import java.util.UUID;

/**
 * ProductVariant entity for handling product variations (size, color, etc.).
 * Each variant can have its own pricing, inventory, and attributes.
 */
@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_product_variant_product", columnList = "product_id"),
    @Index(name = "idx_product_variant_sku", columnList = "variant_sku"),
    @Index(name = "idx_product_variant_active", columnList = "is_active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "variant_sku", nullable = false, unique = true, length = 50)
    private String variantSku;

    @Column(name = "name", nullable = false, length = 200)
    private String name; // e.g., "Red - Large", "128GB - Space Gray"

    // Variant attributes
    @Column(name = "option1_name", length = 50)
    private String option1Name; // e.g., "Color"

    @Column(name = "option1_value", length = 100)
    private String option1Value; // e.g., "Red"

    @Column(name = "option2_name", length = 50)
    private String option2Name; // e.g., "Size"

    @Column(name = "option2_value", length = 100)
    private String option2Value; // e.g., "Large"

    @Column(name = "option3_name", length = 50)
    private String option3Name; // e.g., "Material"

    @Column(name = "option3_value", length = 100)
    private String option3Value; // e.g., "Cotton"

    // Pricing (can override product pricing)
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 10, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    // Inventory
    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    // Physical attributes (can override product attributes)
    @Column(name = "weight", precision = 8, scale = 3)
    private BigDecimal weight;

    @Column(name = "barcode", length = 50)
    private String barcode;

    // Status
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false; // One variant can be marked as default

    // Images specific to this variant
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Position for ordering variants
    @Column(name = "position")
    private Integer position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

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
        if (isDefault == null) {
            isDefault = false;
        }
        if (position == null) {
            position = 0;
        }
    }

    // Business logic methods

    /**
     * Get effective price (variant price or fallback to product price)
     */
    public BigDecimal getEffectivePrice() {
        if (price != null) {
            return price;
        }
        return product != null ? product.getPrice() : BigDecimal.ZERO;
    }

    /**
     * Get effective compare price
     */
    public BigDecimal getEffectiveComparePrice() {
        if (comparePrice != null) {
            return comparePrice;
        }
        return product != null ? product.getComparePrice() : null;
    }

    /**
     * Check if variant is in stock
     */
    public boolean isInStock() {
        if (stockQuantity == null) {
            return product != null && product.isInStock();
        }
        return stockQuantity > 0 || (product != null && product.getAllowBackorder());
    }

    /**
     * Check if variant is low on stock
     */
    public boolean isLowStock() {
        if (stockQuantity == null || lowStockThreshold == null) {
            return product != null && product.isLowStock();
        }
        return stockQuantity <= lowStockThreshold && stockQuantity > 0;
    }

    /**
     * Get variant display name combining all options
     */
    public String getDisplayName() {
        StringBuilder displayName = new StringBuilder();
        
        if (option1Value != null && !option1Value.trim().isEmpty()) {
            displayName.append(option1Value);
        }
        
        if (option2Value != null && !option2Value.trim().isEmpty()) {
            if (displayName.length() > 0) displayName.append(" - ");
            displayName.append(option2Value);
        }
        
        if (option3Value != null && !option3Value.trim().isEmpty()) {
            if (displayName.length() > 0) displayName.append(" - ");
            displayName.append(option3Value);
        }
        
        return displayName.length() > 0 ? displayName.toString() : name;
    }

    /**
     * Check if this variant is on sale
     */
    public boolean isOnSale() {
        BigDecimal effectivePrice = getEffectivePrice();
        BigDecimal effectiveComparePrice = getEffectiveComparePrice();
        
        return effectiveComparePrice != null && 
               effectiveComparePrice.compareTo(BigDecimal.ZERO) > 0 && 
               effectivePrice.compareTo(effectiveComparePrice) < 0;
    }

    /**
     * Calculate discount percentage for this variant
     */
    public BigDecimal getDiscountPercentage() {
        BigDecimal effectivePrice = getEffectivePrice();
        BigDecimal effectiveComparePrice = getEffectiveComparePrice();
        
        if (effectiveComparePrice == null || effectiveComparePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (effectivePrice.compareTo(effectiveComparePrice) >= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = effectiveComparePrice.subtract(effectivePrice);
        return discount.divide(effectiveComparePrice, 4, BigDecimal.ROUND_HALF_UP)
                      .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get variant identifier for frontend (combines option values)
     */
    public String getVariantIdentifier() {
        StringBuilder identifier = new StringBuilder();
        
        if (option1Value != null) identifier.append(option1Value.toLowerCase().replaceAll("\\s+", "-"));
        if (option2Value != null) {
            if (identifier.length() > 0) identifier.append("-");
            identifier.append(option2Value.toLowerCase().replaceAll("\\s+", "-"));
        }
        if (option3Value != null) {
            if (identifier.length() > 0) identifier.append("-");
            identifier.append(option3Value.toLowerCase().replaceAll("\\s+", "-"));
        }
        
        return identifier.toString();
    }
}
