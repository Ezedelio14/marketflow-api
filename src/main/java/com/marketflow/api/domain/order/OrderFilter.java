package com.marketflow.api.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record OrderFilter(
        Set<OrderStatus> statuses,
        UUID userId,
        UUID sellerId,
        String orderNumber,
        Instant createdFrom,
        Instant createdTo,
        BigDecimal minTotal,
        BigDecimal maxTotal
) {
    public OrderFilter restrictToUser(UUID forcedUserId) {
        return new OrderFilter(statuses, forcedUserId, sellerId, orderNumber, createdFrom, createdTo, minTotal, maxTotal);
    }

    public OrderFilter restrictToSeller(UUID forcedSellerId) {
        return new OrderFilter(statuses, userId, forcedSellerId, orderNumber, createdFrom, createdTo, minTotal, maxTotal);
    }
}
