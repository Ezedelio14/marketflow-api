package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PendingPaymentState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.PENDING_PAYMENT;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of(OrderAction.PAY, OrderAction.CANCEL);
    }

    @Override
    public OrderStatus pay() {
        return OrderStatus.PAID;
    }

    @Override
    public OrderStatus cancel() {
        return OrderStatus.CANCELLED;
    }
}
