package com.byteBazar.order.web.dto;

import com.byteBazar.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID buyerId,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt
) {}
