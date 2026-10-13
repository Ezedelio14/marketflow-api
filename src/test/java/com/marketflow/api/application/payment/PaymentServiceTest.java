package com.marketflow.api.application.payment;

import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.order.OrderCancellationService;
import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderItem;
import com.marketflow.api.domain.order.OrderRepository;
import com.marketflow.api.domain.order.OrderStatus;
import com.marketflow.api.domain.order.ShippingAddress;
import com.marketflow.api.domain.order.state.CancelledState;
import com.marketflow.api.domain.order.state.DeliveredState;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import com.marketflow.api.domain.order.state.PaidState;
import com.marketflow.api.domain.order.state.PendingPaymentState;
import com.marketflow.api.domain.order.state.ProcessingState;
import com.marketflow.api.domain.order.state.RefundRequestedState;
import com.marketflow.api.domain.order.state.RefundedState;
import com.marketflow.api.domain.order.state.ShippedState;
import com.marketflow.api.domain.payment.Payment;
import com.marketflow.api.domain.payment.PaymentRepository;
import com.marketflow.api.domain.payment.PaymentStatus;
import com.marketflow.api.domain.payment.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final InventoryService inventoryService = mock(InventoryService.class);
    private final OrderCancellationService cancellationService = mock(OrderCancellationService.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private final EventPublisher publisher = mock(EventPublisher.class);

    private PaymentService service;

    @BeforeEach
    void setUp() {
        OrderStateMachine stateMachine = new OrderStateMachine(List.of(new PendingPaymentState(), new PaidState(),
                new ProcessingState(), new ShippedState(), new DeliveredState(), new CancelledState(),
                new RefundRequestedState(), new RefundedState()));
        service = new PaymentService(paymentRepository, processedEventRepository, orderRepository, stateMachine,
                inventoryService, cancellationService, gateway, publisher, new DomainEventFactory(Clock.systemUTC()), Clock.systemUTC());
        when(gateway.providerName()).thenReturn("stripe");
    }

    @Test
    void succeededWebhookPaysOrderAndCommitsStock() {
        Order order = order();
        Payment payment = Payment.initiate(UUID.randomUUID(), "stripe", "pi_123", order.getTotal(), "BRL");
        when(processedEventRepository.existsById("evt_1")).thenReturn(false);
        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(payment));
        when(orderRepository.findWithItemsById(any())).thenReturn(Optional.of(order));

        PaymentService.Outcome outcome = service.handleWebhook(
                new PaymentWebhookEvent("evt_1", "payment_intent.succeeded", PaymentWebhookEvent.Kind.PAYMENT_SUCCEEDED, "pi_123", null));

        assertThat(outcome).isEqualTo(PaymentService.Outcome.PROCESSED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(inventoryService).confirm(order.getId());
    }

    @Test
    void duplicateWebhookIsSkipped() {
        when(processedEventRepository.existsById("evt_dup")).thenReturn(true);

        PaymentService.Outcome outcome = service.handleWebhook(
                new PaymentWebhookEvent("evt_dup", "payment_intent.succeeded", PaymentWebhookEvent.Kind.PAYMENT_SUCCEEDED, "pi_123", null));

        assertThat(outcome).isEqualTo(PaymentService.Outcome.DUPLICATE);
        verify(paymentRepository, never()).findByProviderPaymentId(any());
        verify(processedEventRepository, never()).save(any());
    }

    private static Order order() {
        return Order.builder()
                .userId(UUID.randomUUID()).orderNumber("MF-1").currency("BRL")
                .shippingAddress(new ShippingAddress("Jane", "1", "St", "1", null, null, "SP", "SP", "0", "BR"))
                .item(OrderItem.of(UUID.randomUUID(), UUID.randomUUID(), "Phone", "SKU", 1, new BigDecimal("99.90")))
                .build();
    }
}
