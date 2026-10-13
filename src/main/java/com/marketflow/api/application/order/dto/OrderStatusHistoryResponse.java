package com.marketflow.api.application.order.dto;

import com.marketflow.api.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "State transition audit entry")
public record OrderStatusHistoryResponse(
        @Schema(description = "Previous status (null for creation)", example = "PENDING_PAYMENT") OrderStatus fromStatus,
        @Schema(description = "New status", example = "PAID") OrderStatus toStatus,
        @Schema(description = "Reason", example = "Payment confirmed by provider") String reason,
        @Schema(description = "Actor that triggered the change", example = "stripe-webhook") String changedBy,
        @Schema(description = "When it happened") Instant changedAt
) {
}
