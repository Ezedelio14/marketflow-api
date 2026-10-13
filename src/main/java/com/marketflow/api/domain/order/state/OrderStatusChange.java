package com.marketflow.api.domain.order.state;

import com.marketflow.api.domain.order.OrderStatus;

import java.util.UUID;

public record OrderStatusChange(UUID orderId, OrderStatus from, OrderStatus to, OrderAction action) {
}
