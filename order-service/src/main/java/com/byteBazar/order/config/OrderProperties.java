package com.byteBazar.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "order")
public class OrderProperties {

    private final Outbox outbox = new Outbox();
    private final Messaging messaging = new Messaging();

    public Outbox getOutbox() { return outbox; }
    public Messaging getMessaging() { return messaging; }

    public static class Outbox {
        private int publishBatchSize = 50;
        private long publisherFixedDelayMs = 2000;

        public int getPublishBatchSize() { return publishBatchSize; }
        public void setPublishBatchSize(int publishBatchSize) { this.publishBatchSize = publishBatchSize; }
        public long getPublisherFixedDelayMs() { return publisherFixedDelayMs; }
        public void setPublisherFixedDelayMs(long publisherFixedDelayMs) { this.publisherFixedDelayMs = publisherFixedDelayMs; }
    }

    public static class Messaging {
        private String region = "us-east-1";
        private String endpoint = "http://localhost:4566";
        private String queueUrl = "http://localhost:4566/000000000000/order-events";
        private String paymentQueueUrl = "http://localhost:4566/000000000000/payment-events";
        private long paymentPollDelayMs = 5000;

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getQueueUrl() { return queueUrl; }
        public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }
        public String getPaymentQueueUrl() { return paymentQueueUrl; }
        public void setPaymentQueueUrl(String paymentQueueUrl) { this.paymentQueueUrl = paymentQueueUrl; }
        public long getPaymentPollDelayMs() { return paymentPollDelayMs; }
        public void setPaymentPollDelayMs(long paymentPollDelayMs) { this.paymentPollDelayMs = paymentPollDelayMs; }
    }
}
