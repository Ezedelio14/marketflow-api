package com.marketflow.api.application.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Order line (snapshot taken at checkout)")
public record OrderItemResponse(
        @Schema(description = "Order item id") UUID id,
        @Schema(description = "Variant id") UUID productVariantId,
        @Schema(description = "Seller responsible for fulfilment") UUID sellerId,
        @Schema(description = "Product name at purchase time", example = "Phone X1") String productName,
        @Schema(description = "SKU", example = "PHN-X1-128-BLK") String sku,
        @Schema(description = "Quantity", example = "1") int quantity,
        @Schema(description = "Unit price", example = "2499.90") BigDecimal unitPrice,
        @Schema(description = "quantity x unitPrice", example = "2499.90") BigDecimal subtotal
) {
}
