package com.byteBazar.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreateRequest(
        @NotNull UUID sellerId,
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price
) {}
