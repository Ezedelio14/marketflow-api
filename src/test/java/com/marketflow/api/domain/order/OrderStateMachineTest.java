package com.marketflow.api.domain.order;

import com.marketflow.api.domain.common.exception.InvalidStateTransitionException;
import com.marketflow.api.domain.order.state.CancelledState;
import com.marketflow.api.domain.order.state.DeliveredState;
import com.marketflow.api.domain.order.state.OrderAction;
import com.marketflow.api.domain.order.state.OrderStateMachine;
import com.marketflow.api.domain.order.state.PaidState;
import com.marketflow.api.domain.order.state.PendingPaymentState;
import com.marketflow.api.domain.order.state.ProcessingState;
import com.marketflow.api.domain.order.state.RefundRequestedState;
import com.marketflow.api.domain.order.state.RefundedState;
import com.marketflow.api.domain.order.state.ShippedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine(List.of(new PendingPaymentState(), new PaidState(), new ProcessingState(),
                new ShippedState(), new DeliveredState(), new CancelledState(), new RefundRequestedState(), new RefundedState()));
    }

    @Test
    void happyPathReachesDeliveredAndRecordsHistory() {
        Order order = newOrder();

        stateMachine.fire(order, OrderAction.PAY, "paid", "test");
        stateMachine.fire(order, OrderAction.START_PROCESSING, "picking", "test");
        stateMachine.fire(order, OrderAction.SHIP, "shipped", "test");
        stateMachine.fire(order, OrderAction.DELIVER, "delivered", "test");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatusHistory()).extracting(OrderStatusHistory::getToStatus).containsExactly(
                OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        assertThat(order.getStatusHistory().get(1).getFromStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void pendingOrderCanBeCancelled() {
        Order order = newOrder();
        stateMachine.fire(order, OrderAction.CANCEL, "customer", "test");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stateMachine.allowedActions(order)).isEmpty();
    }

    @Test
    void paidOrderFollowsRefundFlow() {
        Order order = newOrder();
        stateMachine.fire(order, OrderAction.PAY, null, "test");
        stateMachine.fire(order, OrderAction.REQUEST_REFUND, "not needed", "test");
        stateMachine.fire(order, OrderAction.REFUND, "refunded", "test");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void cannotShipUnpaidOrder() {
        Order order = newOrder();
        assertThatThrownBy(() -> stateMachine.fire(order, OrderAction.SHIP, null, "test"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("SHIP").hasMessageContaining("PENDING_PAYMENT");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getStatusHistory()).hasSize(1);
    }

    @Test
    void cannotCancelPaidOrder() {
        Order order = newOrder();
        stateMachine.fire(order, OrderAction.PAY, null, "test");
        assertThatThrownBy(() -> stateMachine.fire(order, OrderAction.CANCEL, null, "test"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderAction.class, names = {"PAY", "CANCEL"}, mode = EnumSource.Mode.EXCLUDE)
    void pendingPaymentRejectsEverythingButPayAndCancel(OrderAction action) {
        Order order = newOrder();
        assertThat(stateMachine.canFire(order, action)).isFalse();
        assertThatThrownBy(() -> stateMachine.fire(order, action, null, "test")).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void builderComputesTotals() {
        Order order = Order.builder()
                .userId(UUID.randomUUID()).orderNumber("MF-TEST-1").currency("brl")
                .shippingAddress(address())
                .item(OrderItem.of(UUID.randomUUID(), UUID.randomUUID(), "Phone", "SKU-1", 2, new BigDecimal("100.00")))
                .item(OrderItem.of(UUID.randomUUID(), UUID.randomUUID(), "Case", "SKU-2", 1, new BigDecimal("25.50")))
                .discount(new BigDecimal("20.00"))
                .shippingCost(new BigDecimal("15.00"))
                .tax(new BigDecimal("1.05"))
                .build();
        assertThat(order.getSubtotal()).isEqualByComparingTo("225.50");
        assertThat(order.getTotal()).isEqualByComparingTo("221.55");
        assertThat(order.getCurrency()).isEqualTo("BRL");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    private static Order newOrder() {
        return Order.builder()
                .userId(UUID.randomUUID()).orderNumber("MF-TEST").currency("BRL").shippingAddress(address())
                .item(OrderItem.of(UUID.randomUUID(), UUID.randomUUID(), "Phone", "SKU-1", 1, new BigDecimal("10.00")))
                .build();
    }

    private static ShippingAddress address() {
        return new ShippingAddress("Jane", "+55", "Street", "1", null, "Center", "São Paulo", "SP", "01000-000", "BR");
    }
}
