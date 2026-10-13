package com.marketflow.api.application.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Shopping cart (served from Redis cache, persisted in PostgreSQL)")
public record CartResponse(
        @Schema(description = "Cart id") UUID id,
        @Schema(description = "Owner user id") UUID userId,
        @Schema(description = "Cart lines") List<CartItemResponse> items,
        @Schema(description = "Total units", example = "3") int itemCount,
        @Schema(description = "Sum of line totals", example = "5249.70") BigDecimal subtotal,
        @Schema(description = "Last change instant") Instant updatedAt
) {
}
