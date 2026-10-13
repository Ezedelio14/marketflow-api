package com.marketflow.api.application.order.dto;

import com.marketflow.api.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Order row for listings")
public record OrderSummaryResponse(
        @Schema(description = "Order id") UUID id,
        @Schema(description = "Order number", example = "MF-261013-7Q2K9XPA") String orderNumber,
        @Schema(description = "Customer id") UUID userId,
        @Schema(description = "Status", example = "SHIPPED") OrderStatus status,
        @Schema(description = "Total", example = "4749.80") BigDecimal total,
        @Schema(description = "Currency", example = "BRL") String currency,
        @Schema(description = "Creation instant") Instant createdAt
) {
}
