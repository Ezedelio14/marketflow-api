package com.marketflow.api.infrastructure.config;

import com.marketflow.api.application.event.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kafka topology and failure handling.
 *
 * <p>Every consumer failure is retried with exponential back-off (1s, 2s, 4s). When retries are exhausted — or
 * immediately for poison messages that cannot be deserialised — the record is published to {@code <topic>.DLT}
 * with the exception headers attached, so no event is ever silently lost.</p>
 */
@Slf4j
@Configuration
public class KafkaConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    public KafkaAdmin.NewTopics marketflowTopics() {
        List<NewTopic> topics = new ArrayList<>();
        for (String topic : KafkaTopics.ALL) {
            topics.add(TopicBuilder.name(topic).partitions(PARTITIONS).replicas(REPLICAS).build());
            topics.add(TopicBuilder.name(topic + KafkaTopics.DLT_SUFFIX).partitions(1).replicas(REPLICAS).build());
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    /** Picked up by Spring Boot's default listener container factory. */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, -1));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Retry {} for {}-{}@{}: {}", attempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        return handler;
    }

    /** Dead-letter topics are read as raw strings so even undeserialisable payloads can be inspected. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltListenerContainerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "marketflow-dlt-monitor");
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
        return factory;
    }
}
