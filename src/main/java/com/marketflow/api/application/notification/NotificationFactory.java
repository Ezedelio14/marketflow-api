package com.marketflow.api.application.notification;

import com.marketflow.api.application.event.Events.OrderCancelledEvent;
import com.marketflow.api.application.event.Events.OrderCreatedEvent;
import com.marketflow.api.application.event.Events.OrderPaidEvent;
import com.marketflow.api.application.event.Events.OrderShippedEvent;
import com.marketflow.api.application.event.Events.PaymentFailedEvent;
import com.marketflow.api.domain.notification.Notification;
import com.marketflow.api.domain.notification.NotificationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Factory pattern: turns integration events into user-facing {@link Notification}s, keeping every message
 * template in one place.
 */
@Component
public class NotificationFactory {

    public Notification create(OrderCreatedEvent e) {
        return Notification.of(e.userId(), NotificationType.ORDER_CREATED,
                "Order %s received".formatted(e.orderNumber()),
                "We received your order %s totalling %s. Complete the payment to confirm it."
                        .formatted(e.orderNumber(), money(e.total(), e.currency())));
    }

    public Notification create(OrderPaidEvent e) {
        return Notification.of(e.userId(), NotificationType.ORDER_PAID,
                "Payment confirmed for %s".formatted(e.orderNumber()),
                "Payment of %s was confirmed. The seller is preparing your order."
                        .formatted(money(e.amount(), e.currency())));
    }

    public Notification create(OrderCancelledEvent e) {
        return Notification.of(e.userId(), NotificationType.ORDER_CANCELLED,
                "Order %s cancelled".formatted(e.orderNumber()),
                "Your order %s was cancelled. Reason: %s".formatted(e.orderNumber(), e.reason()));
    }

    public Notification create(OrderShippedEvent e) {
        return Notification.of(e.userId(), NotificationType.ORDER_SHIPPED,
                "Order %s shipped".formatted(e.orderNumber()),
                "Your order is on its way with %s. Tracking code: %s".formatted(e.carrier(), e.trackingNumber()));
    }

    public Notification create(PaymentFailedEvent e) {
        return Notification.of(e.userId(), NotificationType.PAYMENT_FAILED,
                "Payment failed",
                "We could not process your payment%s. You can retry before the reservation expires."
                        .formatted(e.reason() == null ? "" : " (" + e.reason() + ")"));
    }

    private static String money(BigDecimal amount, String currency) {
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
