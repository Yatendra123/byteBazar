package com.byteBazar.payment.provider;

import com.byteBazar.payment.config.PaymentProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Primary
@Component
public class PrimaryPaymentProvider implements PaymentProvider {

    private final PaymentProperties properties;

    public PrimaryPaymentProvider(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentResult charge(UUID orderId, BigDecimal amount, String currency) throws Exception {
        if (properties.getDemo().isFailPrimary()) {
            throw new RuntimeException("Primary provider simulated failure");
        }
        return PaymentResult.ok("primary");
    }
}
