package com.byteBazar.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentProvider {
    PaymentResult charge(UUID orderId, BigDecimal amount, String currency) throws Exception;
}
