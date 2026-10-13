package com.marketflow.api.application.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Adds a variant to the cart (quantities are merged if already present)")
public record AddCartItemRequest(
        @Schema(description = "Product variant id") @NotNull UUID productVariantId,
        @Schema(description = "Units to add", example = "2") @NotNull @Min(1) @Max(99) Integer quantity
) {
}
