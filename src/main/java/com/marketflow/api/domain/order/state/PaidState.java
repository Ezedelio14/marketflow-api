package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PaidState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.PAID;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of(OrderAction.START_PROCESSING, OrderAction.REQUEST_REFUND);
    }

    @Override
    public OrderStatus startProcessing() {
        return OrderStatus.PROCESSING;
    }

    @Override
    public OrderStatus requestRefund() {
        return OrderStatus.REFUND_REQUESTED;
    }
}
