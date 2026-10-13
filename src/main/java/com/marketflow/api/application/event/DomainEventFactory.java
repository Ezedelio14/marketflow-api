package com.marketflow.api.application.event;

import com.marketflow.api.application.event.Events.IndexOperation;
import com.marketflow.api.application.event.Events.InventoryReleasedEvent;
import com.marketflow.api.application.event.Events.InventoryReservedEvent;
import com.marketflow.api.application.event.Events.NotificationSendEvent;
import com.marketflow.api.application.event.Events.OrderCancelledEvent;
import com.marketflow.api.application.event.Events.OrderCreatedEvent;
import com.marketflow.api.application.event.Events.OrderLine;
import com.marketflow.api.application.event.Events.OrderPaidEvent;
import com.marketflow.api.application.event.Events.OrderShippedEvent;
import com.marketflow.api.application.event.Events.PaymentFailedEvent;
import com.marketflow.api.application.event.Events.PaymentSucceededEvent;
import com.marketflow.api.application.event.Events.ProductIndexedEvent;
import com.marketflow.api.application.event.Events.StockLine;
import com.marketflow.api.domain.inventory.InventoryReservation;
import com.marketflow.api.domain.notification.Notification;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.payment.Payment;
import com.marketflow.api.domain.shipment.Shipment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Factory pattern: the single place where integration events are assembled from aggregates, so every event gets
 * a unique id, a timestamp from the shared {@link Clock} and a consistent payload shape.
 */
@Component
@RequiredArgsConstructor
public class DomainEventFactory {

    private final Clock clock;

    public OrderCreatedEvent orderCreated(Order order) {
        List<OrderLine> lines = order.getItems().stream()
                .map(i -> new OrderLine(i.getProductVariantId(), i.getSellerId(), i.getSku(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(id(), now(), order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getTotal(), order.getCurrency(), lines);
    }

    public OrderPaidEvent orderPaid(Order order) {
        return new OrderPaidEvent(id(), now(), order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getTotal(), order.getCurrency());
    }

    public OrderCancelledEvent orderCancelled(Order order, String reason) {
        return new OrderCancelledEvent(id(), now(), order.getId(), order.getOrderNumber(), order.getUserId(), reason);
    }

    public OrderShippedEvent orderShipped(Order order, Shipment shipment) {
        return new OrderShippedEvent(id(), now(), order.getId(), order.getOrderNumber(), order.getUserId(),
                shipment.getCarrier(), shipment.getTrackingNumber());
    }

    public PaymentSucceededEvent paymentSucceeded(Payment payment, Order order) {
        return new PaymentSucceededEvent(id(), now(), payment.getId(), order.getId(), order.getUserId(),
                payment.getProviderPaymentId(), payment.getAmount(), payment.getCurrency());
    }

    public PaymentFailedEvent paymentFailed(Payment payment, Order order, String reason) {
        return new PaymentFailedEvent(id(), now(), payment.getId(), order.getId(), order.getUserId(),
                payment.getProviderPaymentId(), reason);
    }

    public InventoryReservedEvent inventoryReserved(UUID orderId, List<InventoryReservation> reservations, Instant expiresAt) {
        return new InventoryReservedEvent(id(), now(), orderId, toStockLines(reservations), expiresAt);
    }

    public InventoryReleasedEvent inventoryReleased(UUID orderId, List<InventoryReservation> reservations, String reason) {
        return new InventoryReleasedEvent(id(), now(), orderId, toStockLines(reservations), reason);
    }

    public ProductIndexedEvent productIndexed(UUID productId, IndexOperation operation) {
        return new ProductIndexedEvent(id(), now(), productId, operation);
    }

    public NotificationSendEvent notificationSend(Notification notification) {
        return new NotificationSendEvent(id(), now(), notification.getId(), notification.getUserId(),
                notification.getType().name(), notification.getTitle(), notification.getMessage());
    }

    private static List<StockLine> toStockLines(List<InventoryReservation> reservations) {
        return reservations.stream()
                .map(r -> new StockLine(r.getInventory().getVariant().getProduct().getId(),
                        r.getInventory().getVariant().getId(), r.getQuantity()))
                .toList();
    }

    private UUID id() {
        return UUID.randomUUID();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
