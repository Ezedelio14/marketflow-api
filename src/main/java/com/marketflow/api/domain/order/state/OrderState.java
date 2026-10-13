package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.common.exception.InvalidStateTransitionException;
import com.marketflow.api.domain.order.OrderStatus;

import java.util.Set;

/**
 * State pattern: one implementation per {@link OrderStatus}. Each state only overrides the actions it accepts
 * and returns the next status; everything else is rejected with {@link InvalidStateTransitionException}.
 */
public interface OrderState {

    OrderStatus status();

    Set<OrderAction> allowedActions();

    default OrderStatus pay() { throw reject(OrderAction.PAY); }

    default OrderStatus startProcessing() { throw reject(OrderAction.START_PROCESSING); }

    default OrderStatus ship() { throw reject(OrderAction.SHIP); }

    default OrderStatus deliver() { throw reject(OrderAction.DELIVER); }

    default OrderStatus cancel() { throw reject(OrderAction.CANCEL); }

    default OrderStatus requestRefund() { throw reject(OrderAction.REQUEST_REFUND); }

    default OrderStatus refund() { throw reject(OrderAction.REFUND); }

    private InvalidStateTransitionException reject(OrderAction action) {
        return new InvalidStateTransitionException(status().name(), action.name());
    }
}
