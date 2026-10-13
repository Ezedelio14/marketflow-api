package com.marketflow.api.infrastructure.messaging.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Surfaces dead-lettered events in the logs (hook for alerting / manual replay tooling).
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @KafkaListener(topicPattern = ".*\\.DLT", containerFactory = "dltListenerContainerFactory")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.error("Dead-lettered event on {} key={} cause='{}' payload={}", record.topic(), record.key(),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE), record.value());
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
