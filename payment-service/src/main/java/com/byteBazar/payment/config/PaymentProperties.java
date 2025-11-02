package com.byteBazar.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private final Demo demo = new Demo();
    private final Messaging messaging = new Messaging();

    public Demo getDemo() { return demo; }
    public Messaging getMessaging() { return messaging; }

    public static class Demo {
        private boolean failPrimary = false;
        public boolean isFailPrimary() { return failPrimary; }
        public void setFailPrimary(boolean failPrimary) { this.failPrimary = failPrimary; }
    }

    public static class Messaging {
        private String region = "us-east-1";
        private String endpoint = "http://localhost:4566";
        private String orderQueueUrl = "http://localhost:4566/000000000000/order-events";
        private String paymentQueueUrl = "http://localhost:4566/000000000000/payment-events";

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getOrderQueueUrl() { return orderQueueUrl; }
        public void setOrderQueueUrl(String orderQueueUrl) { this.orderQueueUrl = orderQueueUrl; }
        public String getPaymentQueueUrl() { return paymentQueueUrl; }
        public void setPaymentQueueUrl(String paymentQueueUrl) { this.paymentQueueUrl = paymentQueueUrl; }
    }
}
