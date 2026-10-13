package com.marketflow.api.application.event;

import java.util.List;

/**
 * Kafka topic names. Each topic has a matching dead-letter topic suffixed with {@link #DLT_SUFFIX}.
 */
public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_SHIPPED = "order.shipped";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RELEASED = "inventory.released";
    public static final String PRODUCT_INDEXED = "product.indexed";
    public static final String NOTIFICATION_SEND = "notification.send";

    public static final String DLT_SUFFIX = ".DLT";

    public static final List<String> ALL = List.of(
            ORDER_CREATED, ORDER_PAID, ORDER_CANCELLED, ORDER_SHIPPED,
            PAYMENT_SUCCEEDED, PAYMENT_FAILED,
            INVENTORY_RESERVED, INVENTORY_RELEASED,
            PRODUCT_INDEXED, NOTIFICATION_SEND
    );

    private KafkaTopics() {
    }
}
