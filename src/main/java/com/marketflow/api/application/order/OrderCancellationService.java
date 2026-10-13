package com.marketflow.api.application.order;

import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.payment.PaymentGateway;
import com.marketflow.api.domain.coupon.CouponRepository;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.state.OrderAction;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import com.marketflow.api.domain.payment.Payment;
import com.marketflow.api.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single cancellation routine shared by the customer endpoint, the reservation-expiry job, payment-cancelled
 * webhooks and the checkout compensation path: transition → release stock → give coupon back → void payment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderStateMachine stateMachine;
    private final InventoryService inventoryService;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(Order order, String reason, String actor, boolean voidProviderPayment) {
        stateMachine.fire(order, OrderAction.CANCEL, reason, actor);
        inventoryService.release(order.getId(), reason);
        if (order.getCouponCode() != null) {
            couponRepository.decrementUsage(order.getCouponCode());
        }
        paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).ifPresent(payment -> {
            payment.markCancelled();
            if (voidProviderPayment) {
                voidAtProvider(payment);
            }
        });
        eventPublisher.publish(eventFactory.orderCancelled(order, reason));
        log.info("Order {} cancelled by {}: {}", order.getOrderNumber(), actor, reason);
    }

    private void voidAtProvider(Payment payment) {
        if (payment.getProviderPaymentId() == null) return;
        try {
            paymentGateway.cancelPaymentIntent(payment.getProviderPaymentId());
        } catch (RuntimeException e) {
            // Best effort: an un-cancelled intent that later succeeds is auto-refunded by the webhook handler.
            log.warn("Could not cancel payment intent {}: {}", payment.getProviderPaymentId(), e.getMessage());
        }
    }
}
