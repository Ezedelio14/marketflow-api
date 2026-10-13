package com.marketflow.api.application.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Sets the physical on-hand quantity of a variant")
public record AdjustInventoryRequest(
        @Schema(description = "New on-hand quantity (must be >= reserved units)", example = "250")
        @NotNull @Min(0) @Max(10_000_000) Integer quantity
) {
}
