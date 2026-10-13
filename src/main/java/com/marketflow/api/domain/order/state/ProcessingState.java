package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProcessingState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.PROCESSING;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of(OrderAction.SHIP);
    }

    @Override
    public OrderStatus ship() {
        return OrderStatus.SHIPPED;
    }
}
