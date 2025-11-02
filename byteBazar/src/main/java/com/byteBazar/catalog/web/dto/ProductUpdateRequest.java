package com.byteBazar.catalog.web.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String title,
        String description,
        BigDecimal price
) {}
