package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RefundedState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.REFUNDED;
    }

    @Override
    public Set<OrderAction> allowedActions() {
        return Set.of();
    }
}
