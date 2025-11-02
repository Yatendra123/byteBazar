package com.byteBazar.payment.messaging;

import com.byteBazar.payment.config.PaymentProperties;
import com.byteBazar.payment.service.PaymentService;
import com.byteBazar.payment.web.dto.PaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderConsumer.class);

    private final SqsAsyncClient sqs;
    private final PaymentProperties props;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentOrderConsumer(SqsAsyncClient sqs, PaymentProperties props, PaymentService paymentService, ObjectMapper objectMapper) {
        this.sqs = sqs;
        this.props = props;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${payment.messaging.poll-delay-ms:5000}")
    public void poll() {
        String queueUrl = props.getMessaging().getOrderQueueUrl();
        try {
            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(5)
                    .maxNumberOfMessages(10)
                    .messageAttributeNames("All")
                    .build();
            List<Message> messages = sqs.receiveMessage(req).join().messages();
            for (Message m : messages) {
                handleMessage(queueUrl, m);
            }
        } catch (Exception ex) {
            log.warn("SQS poll error: {}", ex.toString());
        }
    }

    private void handleMessage(String queueUrl, Message m) {
        Map<String, MessageAttributeValue> attrs = m.messageAttributes();
        String eventType = attrs.getOrDefault("eventType", MessageAttributeValue.builder().stringValue("").dataType("String").build()).stringValue();
        try {
            String body = m.body();
            String encoding = attrs.getOrDefault("encoding", MessageAttributeValue.builder().stringValue("").dataType("String").build()).stringValue();
            if ("base64".equalsIgnoreCase(encoding)) {
                body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
            }

            if (!"OrderCreated".equals(eventType)) {
                log.debug("Skipping eventType={} messageId={}", eventType, m.messageId());
                // Ack skip to avoid backlog
                ack(queueUrl, m);
                return;
            }

            JsonNode root = objectMapper.readTree(body);
            UUID orderId = UUID.fromString(root.get("orderId").asText());
            BigDecimal total = root.hasNonNull("total") ? root.get("total").decimalValue() : BigDecimal.ZERO;
            String currency = root.hasNonNull("currency") ? root.get("currency").asText() : "USD";

            PaymentRequest paymentRequest = new PaymentRequest(orderId, total, currency);
            paymentService.charge(paymentRequest);
            ack(queueUrl, m);
        } catch (Exception ex) {
            log.warn("Failed processing SQS message id={} eventType={} err={}", m.messageId(), eventType, ex.toString());
            // do not delete -> redelivery
        }
    }

    private void ack(String queueUrl, Message m) {
        sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(m.receiptHandle())
                .build()).join();
    }
}
