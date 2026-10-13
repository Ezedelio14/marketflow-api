package com.marketflow.api.application.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Cancellation request (only allowed while PENDING_PAYMENT)")
public record CancelOrderRequest(
        @Schema(description = "Why the order is being cancelled", example = "Changed my mind") @Size(max = 255) String reason
) {
}
