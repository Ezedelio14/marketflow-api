package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.Order;
import com.marketflow.api.domain.order.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context of the State pattern. Resolves the {@link OrderState} for the order's current status, asks it for the
 * next status (invalid actions throw {@code InvalidStateTransitionException}) and applies the change, which also
 * appends an entry to the order's status history.
 *
 * <pre>
 * PENDING_PAYMENT ─PAY→ PAID ─START_PROCESSING→ PROCESSING ─SHIP→ SHIPPED ─DELIVER→ DELIVERED
 *        │                 │
 *        └─CANCEL→ CANCELLED└─REQUEST_REFUND→ REFUND_REQUESTED ─REFUND→ REFUNDED
 * </pre>
 */
@Component
public class OrderStateMachine {

    private final Map<OrderStatus, OrderState> states = new EnumMap<>(OrderStatus.class);

    public OrderStateMachine(List<OrderState> stateBeans) {
        stateBeans.forEach(state -> states.put(state.status(), state));
        for (OrderStatus status : OrderStatus.values()) {
            if (!states.containsKey(status)) {
                throw new IllegalStateException("No OrderState registered for " + status);
            }
        }
    }

    public OrderStatusChange fire(Order order, OrderAction action, String reason, String actor) {
        OrderState current = stateOf(order.getStatus());
        OrderStatus target = switch (action) {
            case PAY -> current.pay();
            case START_PROCESSING -> current.startProcessing();
            case SHIP -> current.ship();
            case DELIVER -> current.deliver();
            case CANCEL -> current.cancel();
            case REQUEST_REFUND -> current.requestRefund();
            case REFUND -> current.refund();
        };
        OrderStatus from = order.getStatus();
        order.applyTransition(target, reason, actor);
        return new OrderStatusChange(order.getId(), from, target, action);
    }

    public boolean canFire(Order order, OrderAction action) {
        return stateOf(order.getStatus()).allowedActions().contains(action);
    }

    public Set<OrderAction> allowedActions(Order order) {
        return stateOf(order.getStatus()).allowedActions();
    }

    private OrderState stateOf(OrderStatus status) {
        return states.get(status);
    }
}
