package com.marketflow.api.domain.order.state;

/**
 * Triggers that drive the order lifecycle.
 */
public enum OrderAction {
    PAY,
    START_PROCESSING,
    SHIP,
    DELIVER,
    CANCEL,
    REQUEST_REFUND,
    REFUND
}
