package com.marketflow.api.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * Idempotency ledger for inbound webhooks. The provider event id is the primary key, so a duplicated delivery
 * (Stripe retries, concurrent deliveries) violates the PK and is processed at most once.
 */
@Getter
@Entity
@Table(name = "processed_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent implements Persistable<String> {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Transient
    private boolean isNew = true;

    public static ProcessedEvent of(String eventId, String provider, String eventType) {
        ProcessedEvent event = new ProcessedEvent();
        event.eventId = eventId;
        event.provider = provider;
        event.eventType = eventType;
        event.processedAt = Instant.now();
        return event;
    }

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
