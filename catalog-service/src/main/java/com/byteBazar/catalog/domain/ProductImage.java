package com.byteBazar.catalog.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * ProductImage entity for storing product images with metadata.
 * Supports multiple images per product with ordering and primary image designation.
 */
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_image_product", columnList = "product_id"),
    @Index(name = "idx_product_image_primary", columnList = "is_primary"),
    @Index(name = "idx_product_image_order", columnList = "display_order")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "file_size")
    private Long fileSize; // in bytes

    @Column(name = "width")
    private Integer width; // in pixels

    @Column(name = "height")
    private Integer height; // in pixels

    @Column(name = "content_type", length = 50)
    private String contentType; // MIME type

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isPrimary == null) {
            isPrimary = false;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    /**
     * Check if this is a valid image based on content type
     */
    public boolean isValidImageType() {
        if (contentType == null) return false;
        return contentType.startsWith("image/");
    }

    /**
     * Get formatted file size string
     */
    public String getFormattedFileSize() {
        if (fileSize == null) return "Unknown";
        
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Get aspect ratio as a formatted string
     */
    public String getAspectRatio() {
        if (width == null || height == null || height == 0) {
            return "Unknown";
        }
        
        double ratio = (double) width / height;
        return String.format("%.2f:1", ratio);
    }
}
