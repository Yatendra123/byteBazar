package com.byteBazar.payment.service;

import com.byteBazar.payment.domain.PaymentEntity;
import com.byteBazar.payment.domain.PaymentStatus;
import com.byteBazar.payment.messaging.PaymentEventPublisher;
import com.byteBazar.payment.provider.PaymentProvider;
import com.byteBazar.payment.provider.PaymentResult;
import com.byteBazar.payment.repository.PaymentRepository;
import com.byteBazar.payment.web.dto.PaymentRequest;
import com.byteBazar.payment.web.dto.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider primary;
    private final PaymentProvider secondary;
    private final PaymentEventPublisher publisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProvider primary,
                          @Qualifier("secondaryPaymentProvider") PaymentProvider secondary,
                          PaymentEventPublisher publisher) {
        this.paymentRepository = paymentRepository;
        this.primary = primary;
        this.secondary = secondary;
        this.publisher = publisher;
    }

    @Transactional
    @CircuitBreaker(name = "primaryProvider", fallbackMethod = "fallbackCharge")
    @Retry(name = "primaryProvider")
    public PaymentResponse charge(PaymentRequest req) throws Exception {
        // Idempotency: if latest for order is COMPLETED, return it
        Optional<PaymentEntity> existing = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(req.orderId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.COMPLETED) {
            PaymentEntity e = existing.get();
            return new PaymentResponse(e.getId(), e.getOrderId(), e.getAmount(), e.getCurrency(), e.getStatus(), e.getProvider(), e.getFailureReason(), e.getCreatedAt());
        }
        // Attempt primary
        PaymentResult result = primary.charge(req.orderId(), req.amount(), req.currency());
        PaymentEntity p = new PaymentEntity();
        p.setId(UUID.randomUUID());
        p.setOrderId(req.orderId());
        p.setAmount(req.amount());
        p.setCurrency(req.currency());
        if (result.success()) {
            p.setStatus(PaymentStatus.COMPLETED);
            p.setProvider(result.provider());
            p.setFailureReason(null);
        } else {
            p.setStatus(PaymentStatus.FAILED);
            p.setProvider(result.provider());
            p.setFailureReason(result.failureReason());
        }
        paymentRepository.save(p);
        publisher.publishPaymentEvent(p);
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getFailureReason(), p.getCreatedAt());
    }

    @Transactional
    public PaymentResponse fallbackCharge(PaymentRequest req, Throwable ex) throws Exception {
        // Idempotency check here too
        Optional<PaymentEntity> existing = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(req.orderId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.COMPLETED) {
            PaymentEntity e = existing.get();
            return new PaymentResponse(e.getId(), e.getOrderId(), e.getAmount(), e.getCurrency(), e.getStatus(), e.getProvider(), e.getFailureReason(), e.getCreatedAt());
        }
        // Secondary fallback creates the record once
        PaymentEntity p = new PaymentEntity();
        p.setId(UUID.randomUUID());
        p.setOrderId(req.orderId());
        p.setAmount(req.amount());
        p.setCurrency(req.currency());
        PaymentResult result = secondary.charge(req.orderId(), req.amount(), req.currency());
        if (result.success()) {
            p.setStatus(PaymentStatus.COMPLETED);
            p.setProvider(result.provider());
            p.setFailureReason(null);
        } else {
            p.setStatus(PaymentStatus.FAILED);
            p.setProvider(result.provider());
            p.setFailureReason(result.failureReason());
        }
        paymentRepository.save(p);
        publisher.publishPaymentEvent(p);
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getFailureReason(), p.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        PaymentEntity p = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getFailureReason(), p.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getLatestByOrder(UUID orderId) {
        PaymentEntity p = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId));
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency(), p.getStatus(), p.getProvider(), p.getFailureReason(), p.getCreatedAt());
    }
}
