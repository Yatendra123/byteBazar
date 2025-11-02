package com.byteBazar.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.byteBazar.order.domain.OrderEntity;
import com.byteBazar.order.domain.OrderItemEntity;
import com.byteBazar.order.domain.OrderStatus;
import com.byteBazar.order.domain.OutboxEntity;
import com.byteBazar.order.repository.OrderRepository;
import com.byteBazar.order.repository.OutboxRepository;
import com.byteBazar.order.web.dto.OrderCreateRequest;
import com.byteBazar.order.web.dto.OrderItemRequest;
import com.byteBazar.order.web.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse createOrder(String idempotencyKey, OrderCreateRequest req) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .map(OrderService::toResponse)
                    .orElseGet(() -> doCreate(idempotencyKey, req));
        }
        return doCreate(null, req);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return orderRepository.findById(id)
                .map(OrderService::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private OrderResponse doCreate(String idempotencyKey, OrderCreateRequest req) {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setBuyerId(req.buyerId());
        order.setStatus(OrderStatus.CREATED);
        order.setIdempotencyKey(idempotencyKey);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : req.items()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setId(UUID.randomUUID());
            item.setOrder(order);
            item.setProductId(itemReq.productId());
            item.setQuantity(itemReq.quantity());
            item.setPrice(itemReq.price());
            order.getItems().add(item);
            total = total.add(itemReq.price().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }
        order.setTotal(total);

        orderRepository.save(order);

        // Outbox event
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("buyerId", order.getBuyerId());
        payload.put("total", order.getTotal());
        payload.put("status", order.getStatus().name());
        payload.put("items", req.items());
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize OrderCreated payload", e);
        }

        OutboxEntity out = new OutboxEntity();
        out.setId(UUID.randomUUID());
        out.setAggregateType("Order");
        out.setAggregateId(order.getId());
        out.setEventType("OrderCreated");
        out.setPayload(json);
        outboxRepository.save(out);

        return toResponse(order);
    }

    private static OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(order.getId(), order.getBuyerId(), order.getStatus(), order.getTotal(), order.getCreatedAt());
    }
}
