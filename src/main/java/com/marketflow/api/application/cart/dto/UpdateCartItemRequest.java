package com.marketflow.api.application.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Replaces the quantity of a cart line")
public record UpdateCartItemRequest(
        @Schema(description = "New quantity", example = "3") @NotNull @Min(1) @Max(99) Integer quantity
) {
}
