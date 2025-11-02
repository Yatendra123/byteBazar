package com.byteBazar.payment.messaging;

import com.byteBazar.payment.config.PaymentProperties;
import com.byteBazar.payment.domain.PaymentEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final SqsAsyncClient sqs;
    private final PaymentProperties props;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(SqsAsyncClient sqs, PaymentProperties props, ObjectMapper objectMapper) {
        this.sqs = sqs;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void publishPaymentEvent(PaymentEntity p) {
        String eventType = p.getStatus().name().equals("COMPLETED") ? "PaymentCharged" : "PaymentFailed";
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", p.getId());
        payload.put("orderId", p.getOrderId());
        payload.put("amount", p.getAmount());
        payload.put("currency", p.getCurrency());
        payload.put("status", p.getStatus().name());
        payload.put("provider", p.getProvider());
        payload.put("failureReason", p.getFailureReason());

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payment event: {}", e.toString());
            return;
        }
        Map<String, MessageAttributeValue> attrs = new HashMap<>();
        attrs.put("eventType", MessageAttributeValue.builder().dataType("String").stringValue(eventType).build());
        attrs.put("aggregateType", MessageAttributeValue.builder().dataType("String").stringValue("Payment").build());

        SendMessageRequest req = SendMessageRequest.builder()
                .queueUrl(props.getMessaging().getPaymentQueueUrl())
                .messageBody(body)
                .messageAttributes(attrs)
                .build();
        try {
            sqs.sendMessage(req).join();
        } catch (Exception ex) {
            log.warn("Failed to publish payment event id={} type={} err={}", p.getId(), eventType, ex.toString());
        }
    }
}
