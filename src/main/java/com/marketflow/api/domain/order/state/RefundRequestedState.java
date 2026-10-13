package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RefundRequestedState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.REFUND_REQUESTED;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of(OrderAction.REFUND);
    }

    @Override
    public OrderStatus refund() {
        return OrderStatus.REFUNDED;
    }
}
