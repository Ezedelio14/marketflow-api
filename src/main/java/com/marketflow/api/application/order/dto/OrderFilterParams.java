package com.marketflow.api.application.order.dto;

import com.marketflow.api.domain.order.OrderFilter;
import com.marketflow.api.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Order search filters (query parameters). Customers and sellers are automatically scoped.")
public record OrderFilterParams(
        @Schema(description = "Statuses to include (OR)", example = "[\"PAID\",\"PROCESSING\"]") Set<OrderStatus> status,
        @Schema(description = "Customer id (admin only)") UUID userId,
        @Schema(description = "Seller id (admin only)") UUID sellerId,
        @Schema(description = "Order number prefix", example = "MF-2610") String orderNumber,
        @Schema(description = "Created at or after (ISO-8601)", example = "2026-10-01T00:00:00Z") Instant createdFrom,
        @Schema(description = "Created at or before (ISO-8601)", example = "2026-10-31T23:59:59Z") Instant createdTo,
        @Schema(description = "Minimum total", example = "100.00") BigDecimal minTotal,
        @Schema(description = "Maximum total", example = "5000.00") BigDecimal maxTotal
) {
    public OrderFilter toFilter() {
        return new OrderFilter(status, userId, sellerId, orderNumber, createdFrom, createdTo, minTotal, maxTotal);
    }
}
