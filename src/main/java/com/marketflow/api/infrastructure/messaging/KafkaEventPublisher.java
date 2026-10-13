package com.marketflow.api.infrastructure.messaging;

import com.marketflow.api.application.event.DomainEvent;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.infrastructure.web.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

/**
 * Kafka producer for integration events. Inside a transaction the send is deferred to {@code afterCommit}, so
 * consumers never react to data that was rolled back. The correlation id travels as a record header.
 *
 * <p>For strict at-least-once delivery across crashes between commit and send, this class is the seam where a
 * transactional outbox would plug in.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event, correlationId);
                }
            });
        } else {
            send(event, correlationId);
        }
    }

    private void send(DomainEvent event, String correlationId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(event.topic(), event.key(), event);
        if (correlationId != null) {
            record.headers().add(CorrelationIdFilter.HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        try {
            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} to {}: {}", event.getClass().getSimpleName(), event.topic(), ex.getMessage());
                } else {
                    log.debug("Published {} to {}-{}@{}", event.eventId(), event.topic(),
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException e) {
            log.error("Kafka unavailable, event {} on {} not published: {}", event.eventId(), event.topic(), e.getMessage());
        }
    }
}
