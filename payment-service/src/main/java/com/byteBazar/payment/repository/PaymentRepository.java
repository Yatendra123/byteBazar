package com.byteBazar.payment.repository;

import com.byteBazar.payment.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
