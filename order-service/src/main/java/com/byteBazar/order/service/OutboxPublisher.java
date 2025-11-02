package com.byteBazar.order.service;

import com.byteBazar.order.config.OrderProperties;
import com.byteBazar.order.domain.OutboxEntity;
import com.byteBazar.order.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final SqsAsyncClient sqs;
    private final OrderProperties props;

    public OutboxPublisher(OutboxRepository outboxRepository, SqsAsyncClient sqs, OrderProperties props) {
        this.outboxRepository = outboxRepository;
        this.sqs = sqs;
        this.props = props;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${order.outbox.publisher-fixed-delay-ms:2000}")
    public void publish() {
        List<OutboxEntity> batch = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, props.getOutbox().getPublishBatchSize()));
        if (batch.isEmpty()) {
            return;
        }
        String queueUrl = props.getMessaging().getQueueUrl();
        for (OutboxEntity e : batch) {
            try {
                Map<String, MessageAttributeValue> attrs = new HashMap<>();
                attrs.put("eventType", MessageAttributeValue.builder().dataType("String").stringValue(e.getEventType()).build());
                attrs.put("aggregateType", MessageAttributeValue.builder().dataType("String").stringValue(e.getAggregateType()).build());
                // Optional: base64-encode payload if large or to avoid special chars
                String body = e.getPayload();
                if (body.length() > 240000) { // ~240 KB guard
                    body = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
                    attrs.put("encoding", MessageAttributeValue.builder().dataType("String").stringValue("base64").build());
                }
                SendMessageRequest req = SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .messageAttributes(attrs)
                        .build();
                sqs.sendMessage(req).join();
                e.setPublished(true);
                outboxRepository.save(e);
            } catch (Exception ex) {
                log.warn("Failed to publish outbox id={} type={} retry={} error={}", e.getId(), e.getEventType(), e.getRetries(), ex.toString());
                e.setRetries(e.getRetries() + 1);
                outboxRepository.save(e);
            }
        }
    }
}
