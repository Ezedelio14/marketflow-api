package com.marketflow.api.application.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer refund request (only allowed while PAID)")
public record RefundRequest(
        @Schema(description = "Refund reason", example = "Product no longer needed") @NotBlank @Size(max = 255) String reason
) {
}
