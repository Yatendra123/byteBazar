package com.byteBazar.catalog.web.dto;

import com.byteBazar.catalog.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        BigDecimal price,
        Instant createdAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSellerId(),
                p.getTitle(),
                p.getDescription(),
                p.getPrice(),
                p.getCreatedAt()
        );
    }
}
