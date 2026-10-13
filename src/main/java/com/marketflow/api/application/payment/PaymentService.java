package com.marketflow.api.application.payment;

import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.order.OrderCancellationService;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderRepository;
import com.marketflow.api.domain.order.OrderStatus;
import com.marketflow.api.domain.order.state.OrderAction;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import com.marketflow.api.domain.payment.Payment;
import com.marketflow.api.domain.payment.PaymentRepository;
import com.marketflow.api.domain.payment.ProcessedEvent;
import com.marketflow.api.domain.payment.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Applies provider webhooks to payments and orders exactly once.
 *
 * <p>Idempotency: the provider event id is inserted into {@code processed_events} in the same transaction as the
 * business changes. A redelivery is skipped by the existence check; two concurrent deliveries race on the primary
 * key and the loser rolls back (the provider retries and then hits the existence check).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String WEBHOOK_ACTOR = "payment-webhook";

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final InventoryService inventoryService;
    private final OrderCancellationService cancellationService;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;
    private final Clock clock;

    public enum Outcome { PROCESSED, DUPLICATE, IGNORED }

    @Transactional
    public Outcome handleWebhook(PaymentWebhookEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Skipping duplicate webhook {} ({})", event.eventId(), event.rawType());
            return Outcome.DUPLICATE;
        }
        processedEventRepository.save(ProcessedEvent.of(event.eventId(), paymentGateway.providerName(), event.rawType()));

        if (event.kind() == PaymentWebhookEvent.Kind.IGNORED || event.providerPaymentId() == null) {
            return Outcome.IGNORED;
        }
        Optional<Payment> maybePayment = paymentRepository.findByProviderPaymentId(event.providerPaymentId());
        if (maybePayment.isEmpty()) {
            log.warn("Webhook {} references unknown payment {}", event.eventId(), event.providerPaymentId());
            return Outcome.IGNORED;
        }
        Payment payment = maybePayment.get();
        Order order = orderRepository.findWithItemsById(payment.getOrderId()).orElseThrow();

        switch (event.kind()) {
            case PAYMENT_SUCCEEDED -> onSucceeded(payment, order);
            case PAYMENT_FAILED -> onFailed(payment, order, event.failureReason());
            case PAYMENT_CANCELED -> onCanceled(order);
            case REFUNDED -> onRefunded(payment, order);
            default -> { return Outcome.IGNORED; }
        }
        return Outcome.PROCESSED;
    }

    /** Issues a refund for a REFUND_REQUESTED order. Finalised here if synchronous, otherwise by the webhook. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refund(Order order, String actor) {
        Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                .filter(Payment::isSucceeded)
                .orElseThrow(() -> new BusinessRuleException("NO_CAPTURED_PAYMENT", "Order has no captured payment to refund"));
        PaymentGateway.RefundResult result = paymentGateway.refund(payment.getProviderPaymentId(), payment.getAmount(),
                payment.getCurrency(), "refund-" + order.getId());
        payment.markRefundPending();
        log.info("Refund {} requested for order {} by {} (status {})", result.refundId(), order.getOrderNumber(), actor, result.status());
        if (result.succeeded()) {
            onRefunded(payment, order);
        }
    }

    private void onSucceeded(Payment payment, Order order) {
        if (payment.isSucceeded()) return;
        payment.markSucceeded(Instant.now(clock));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            stateMachine.fire(order, OrderAction.PAY, "Payment confirmed by provider", WEBHOOK_ACTOR);
            inventoryService.confirm(order.getId());
            eventPublisher.publish(eventFactory.paymentSucceeded(payment, order));
            eventPublisher.publish(eventFactory.orderPaid(order));
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            // Paid after the reservation expired: stock is gone, give the money back automatically.
            log.warn("Payment {} succeeded for cancelled order {}: issuing automatic refund", payment.getProviderPaymentId(), order.getOrderNumber());
            paymentGateway.refund(payment.getProviderPaymentId(), payment.getAmount(), payment.getCurrency(), "auto-refund-" + order.getId());
            payment.markRefundPending();
        }
    }

    private void onFailed(Payment payment, Order order, String reason) {
        payment.markFailed(reason);
        eventPublisher.publish(eventFactory.paymentFailed(payment, order, reason));
        // The order stays PENDING_PAYMENT so the customer can retry until the reservation TTL expires.
    }

    private void onCanceled(Order order) {
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            cancellationService.cancel(order, "Payment cancelled at provider", WEBHOOK_ACTOR, false);
        }
    }

    private void onRefunded(Payment payment, Order order) {
        payment.markRefunded();
        if (order.getStatus() == OrderStatus.PAID) {
            // Refund issued directly in the provider dashboard
            stateMachine.fire(order, OrderAction.REQUEST_REFUND, "Refund issued at provider", WEBHOOK_ACTOR);
        }
        if (order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            stateMachine.fire(order, OrderAction.REFUND, "Refund confirmed by provider", WEBHOOK_ACTOR);
        }
    }
}
