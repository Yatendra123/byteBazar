package com.byteBazar.payment.provider;

public record PaymentResult(boolean success, String provider, String failureReason) {
    public static PaymentResult ok(String provider) {
        return new PaymentResult(true, provider, null);
    }
    public static PaymentResult fail(String provider, String reason) {
        return new PaymentResult(false, provider, reason);
    }
}
