package com.marketflow.api.application.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Priced checkout line (current catalog price)")
public record CheckoutLineResponse(
        @Schema(description = "Variant id") UUID productVariantId,
        @Schema(description = "Product name", example = "Phone X1") String productName,
        @Schema(description = "SKU", example = "PHN-X1-128-BLK") String sku,
        @Schema(description = "Quantity", example = "2") int quantity,
        @Schema(description = "Unit price", example = "2499.90") BigDecimal unitPrice,
        @Schema(description = "Line total", example = "4999.80") BigDecimal lineTotal
) {
}
