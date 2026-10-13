package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ShippedState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.SHIPPED;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of(OrderAction.DELIVER);
    }

    @Override
    public OrderStatus deliver() {
        return OrderStatus.DELIVERED;
    }
}
