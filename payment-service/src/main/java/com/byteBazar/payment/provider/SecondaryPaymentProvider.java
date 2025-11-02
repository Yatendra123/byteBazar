package com.byteBazar.payment.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SecondaryPaymentProvider implements PaymentProvider {
    @Override
    public PaymentResult charge(UUID orderId, BigDecimal amount, String currency) throws Exception {
        // Simulate a successful secondary charge
        return PaymentResult.ok("secondary");
    }
}
