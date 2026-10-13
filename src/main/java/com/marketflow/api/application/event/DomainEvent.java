package com.marketflow.api.application.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration event published to Kafka. {@link #topic()} and {@link #key()} are routing metadata and are not
 * serialized (they are not bean getters); the key keeps all events of one aggregate on the same partition.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String topic();

    String key();
}
