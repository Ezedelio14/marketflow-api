package com.marketflow.api.domain.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == REFUNDED;
    }
}
