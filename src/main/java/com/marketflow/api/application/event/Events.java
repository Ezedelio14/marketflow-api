package com.marketflow.api.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payloads of every integration event. Kept in one file because they are plain data contracts.
 */
public final class Events {

    private Events() {
    }

    public record OrderLine(UUID productVariantId, UUID sellerId, String sku, int quantity, BigDecimal unitPrice) {
    }

    public record StockLine(UUID productId, UUID productVariantId, int quantity) {
    }

    public enum IndexOperation { INDEX, DELETE }

    public record OrderCreatedEvent(UUID eventId, Instant occurredAt, UUID orderId, String orderNumber, UUID userId,
                                    BigDecimal total, String currency, List<OrderLine> items) implements DomainEvent {
        public String topic() { return KafkaTopics.ORDER_CREATED; }
        public String key() { return orderId.toString(); }
    }

    public record OrderPaidEvent(UUID eventId, Instant occurredAt, UUID orderId, String orderNumber, UUID userId,
                                 BigDecimal amount, String currency) implements DomainEvent {
        public String topic() { return KafkaTopics.ORDER_PAID; }
        public String key() { return orderId.toString(); }
    }

    public record OrderCancelledEvent(UUID eventId, Instant occurredAt, UUID orderId, String orderNumber, UUID userId,
                                      String reason) implements DomainEvent {
        public String topic() { return KafkaTopics.ORDER_CANCELLED; }
        public String key() { return orderId.toString(); }
    }

    public record OrderShippedEvent(UUID eventId, Instant occurredAt, UUID orderId, String orderNumber, UUID userId,
                                    String carrier, String trackingNumber) implements DomainEvent {
        public String topic() { return KafkaTopics.ORDER_SHIPPED; }
        public String key() { return orderId.toString(); }
    }

    public record PaymentSucceededEvent(UUID eventId, Instant occurredAt, UUID paymentId, UUID orderId, UUID userId,
                                        String providerPaymentId, BigDecimal amount, String currency) implements DomainEvent {
        public String topic() { return KafkaTopics.PAYMENT_SUCCEEDED; }
        public String key() { return orderId.toString(); }
    }

    public record PaymentFailedEvent(UUID eventId, Instant occurredAt, UUID paymentId, UUID orderId, UUID userId,
                                     String providerPaymentId, String reason) implements DomainEvent {
        public String topic() { return KafkaTopics.PAYMENT_FAILED; }
        public String key() { return orderId.toString(); }
    }

    public record InventoryReservedEvent(UUID eventId, Instant occurredAt, UUID orderId, List<StockLine> lines,
                                         Instant expiresAt) implements DomainEvent {
        public String topic() { return KafkaTopics.INVENTORY_RESERVED; }
        public String key() { return orderId.toString(); }
    }

    public record InventoryReleasedEvent(UUID eventId, Instant occurredAt, UUID orderId, List<StockLine> lines,
                                         String reason) implements DomainEvent {
        public String topic() { return KafkaTopics.INVENTORY_RELEASED; }
        public String key() { return orderId.toString(); }
    }

    public record ProductIndexedEvent(UUID eventId, Instant occurredAt, UUID productId,
                                      IndexOperation operation) implements DomainEvent {
        public String topic() { return KafkaTopics.PRODUCT_INDEXED; }
        public String key() { return productId.toString(); }
    }

    public record NotificationSendEvent(UUID eventId, Instant occurredAt, UUID notificationId, UUID userId,
                                        String type, String title, String message) implements DomainEvent {
        public String topic() { return KafkaTopics.NOTIFICATION_SEND; }
        public String key() { return userId.toString(); }
    }
}
