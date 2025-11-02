package com.byteBazar.catalog.web.dto;

import com.byteBazar.catalog.domain.ProductImage;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for ProductImage entities.
 */
@Schema(description = "Product image response with metadata")
public record ProductImageResponse(
    @Schema(description = "Image unique identifier")
    UUID id,
    
    @Schema(description = "Image URL", example = "https://example.com/images/product1.jpg")
    String url,
    
    @Schema(description = "Image alt text for accessibility")
    String altText,
    
    @Schema(description = "Image title")
    String title,
    
    @Schema(description = "Display order", example = "1")
    Integer displayOrder,
    
    @Schema(description = "Whether this is the primary image")
    Boolean isPrimary,
    
    @Schema(description = "File size in bytes")
    Long fileSize,
    
    @Schema(description = "Formatted file size", example = "125.5 KB")
    String formattedFileSize,
    
    @Schema(description = "Image width in pixels")
    Integer width,
    
    @Schema(description = "Image height in pixels")
    Integer height,
    
    @Schema(description = "Image aspect ratio", example = "1.33:1")
    String aspectRatio,
    
    @Schema(description = "Image content type", example = "image/jpeg")
    String contentType,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Image creation timestamp")
    Instant createdAt
) {
    
    public static ProductImageResponse from(ProductImage image) {
        if (image == null) return null;
        
        return new ProductImageResponse(
            image.getId(),
            image.getUrl(),
            image.getAltText(),
            image.getTitle(),
            image.getDisplayOrder(),
            image.getIsPrimary(),
            image.getFileSize(),
            image.getFormattedFileSize(),
            image.getWidth(),
            image.getHeight(),
            image.getAspectRatio(),
            image.getContentType(),
            image.getCreatedAt()
        );
    }
}
