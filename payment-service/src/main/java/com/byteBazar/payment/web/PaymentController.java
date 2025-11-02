package com.byteBazar.payment.web;

import com.byteBazar.payment.service.PaymentService;
import com.byteBazar.payment.web.dto.PaymentRequest;
import com.byteBazar.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> charge(@Valid @RequestBody PaymentRequest request) throws Exception {
        PaymentResponse resp = paymentService.charge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentResponse> getLatestByOrder(@PathVariable UUID orderId) {
        try {
            // Reuse charge idempotency lookup path indirectly by attempting to construct a response
            // A small helper in service would be cleaner, but to keep changes minimal we'll duplicate logic here
            PaymentResponse resp = paymentService.getLatestByOrder(orderId);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
