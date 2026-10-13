package com.marketflow.api.infrastructure.messaging.consumer;

import com.marketflow.api.application.event.Events.OrderCancelledEvent;
import com.marketflow.api.application.event.Events.OrderCreatedEvent;
import com.marketflow.api.application.event.Events.OrderPaidEvent;
import com.marketflow.api.application.event.Events.OrderShippedEvent;
import com.marketflow.api.application.event.Events.PaymentFailedEvent;
import com.marketflow.api.application.event.KafkaTopics;
import com.marketflow.api.application.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Turns order/payment events into customer notifications.
 */
@Component
@RequiredArgsConstructor
public class NotificationEventsConsumer {

    private static final String GROUP = "marketflow-notifications";

    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = GROUP)
    public void onOrderCreated(OrderCreatedEvent event) {
        notificationService.on(event);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = GROUP)
    public void onOrderPaid(OrderPaidEvent event) {
        notificationService.on(event);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = GROUP)
    public void onOrderCancelled(OrderCancelledEvent event) {
        notificationService.on(event);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_SHIPPED, groupId = GROUP)
    public void onOrderShipped(OrderShippedEvent event) {
        notificationService.on(event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = GROUP)
    public void onPaymentFailed(PaymentFailedEvent event) {
        notificationService.on(event);
    }
}
