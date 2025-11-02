package com.byteBazar.order.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull UUID buyerId,
        @NotNull @Valid List<OrderItemRequest> items
) {}
