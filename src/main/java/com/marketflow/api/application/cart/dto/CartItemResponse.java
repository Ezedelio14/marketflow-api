package com.marketflow.api.application.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Cart line")
public record CartItemResponse(
        @Schema(description = "Product variant id") UUID productVariantId,
        @Schema(description = "Product id") UUID productId,
        @Schema(description = "Product name", example = "Phone X1") String productName,
        @Schema(description = "Variant label", example = "128GB / Black") String variantName,
        @Schema(description = "SKU", example = "PHN-X1-128-BLK") String sku,
        @Schema(description = "Quantity", example = "2") int quantity,
        @Schema(description = "Unit price snapshot", example = "2499.90") BigDecimal unitPrice,
        @Schema(description = "quantity x unitPrice", example = "4999.80") BigDecimal lineTotal
) {
}
