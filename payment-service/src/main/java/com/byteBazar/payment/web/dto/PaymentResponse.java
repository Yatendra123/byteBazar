package com.byteBazar.payment.web.dto;

import com.byteBazar.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String provider,
        String failureReason,
        Instant createdAt
) {}
