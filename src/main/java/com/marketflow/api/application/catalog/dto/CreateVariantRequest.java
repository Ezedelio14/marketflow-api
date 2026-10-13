package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Variant (SKU) of a product")
public record CreateVariantRequest(
        @Schema(description = "Unique stock keeping unit", example = "PHN-X1-128-BLK")
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String sku,
        @Schema(description = "Variant label", example = "128GB / Black") @NotBlank @Size(max = 255) String name,
        @Schema(description = "Selling price", example = "2499.90") @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @Schema(description = "Original price shown struck-through", example = "2799.90", nullable = true)
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal compareAtPrice,
        @Schema(description = "Weight in kg (used for shipping)", example = "0.350") @DecimalMin("0.000") @Digits(integer = 7, fraction = 3) BigDecimal weight,
        @Schema(description = "Initial stock on hand", example = "100") @Min(0) int initialStock
) {
}
