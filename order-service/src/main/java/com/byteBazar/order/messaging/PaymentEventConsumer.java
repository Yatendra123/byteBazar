package com.byteBazar.order.messaging;

import com.byteBazar.order.config.OrderProperties;
import com.byteBazar.order.domain.OrderEntity;
import com.byteBazar.order.domain.OrderStatus;
import com.byteBazar.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final SqsAsyncClient sqs;
    private final OrderProperties props;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(SqsAsyncClient sqs, OrderProperties props, OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.sqs = sqs;
        this.props = props;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${order.messaging.payment-poll-delay-ms:5000}")
    public void poll() {
        String queueUrl = props.getMessaging().getPaymentQueueUrl();
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
            log.warn("Payment SQS poll error: {}", ex.toString());
        }
    }

    @Transactional
    protected void handleMessage(String queueUrl, Message m) {
        Map<String, MessageAttributeValue> attrs = m.messageAttributes();
        String eventType = attrs.getOrDefault("eventType", MessageAttributeValue.builder().dataType("String").stringValue("").build()).stringValue();
        try {
            JsonNode root = objectMapper.readTree(m.body());
            UUID orderId = UUID.fromString(root.get("orderId").asText());
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            if ("PaymentCharged".equals(eventType)) {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                ack(queueUrl, m);
            } else if ("PaymentFailed".equals(eventType)) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                ack(queueUrl, m);
            } else {
                log.debug("Skipping eventType={} messageId={}", eventType, m.messageId());
                // Ack skip to avoid backlog
                ack(queueUrl, m);
            }
        } catch (Exception ex) {
            log.warn("Failed processing payment event message id={} eventType={} err={}", m.messageId(), eventType, ex.toString());
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
