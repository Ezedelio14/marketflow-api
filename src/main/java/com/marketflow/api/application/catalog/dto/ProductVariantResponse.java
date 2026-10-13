package com.marketflow.api.application.catalog.dto;

import com.marketflow.api.domain.catalog.VariantStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product variant")
public record ProductVariantResponse(
        @Schema(description = "Variant id") UUID id,
        @Schema(description = "SKU", example = "PHN-X1-128-BLK") String sku,
        @Schema(description = "Variant label", example = "128GB / Black") String name,
        @Schema(description = "Selling price", example = "2499.90") BigDecimal price,
        @Schema(description = "Original price", example = "2799.90") BigDecimal compareAtPrice,
        @Schema(description = "Weight in kg", example = "0.350") BigDecimal weight,
        @Schema(description = "Status", example = "ACTIVE") VariantStatus status
) {
}
