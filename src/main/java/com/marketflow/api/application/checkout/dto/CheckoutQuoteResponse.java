package com.marketflow.api.application.checkout.dto;

import com.marketflow.api.application.pricing.shipping.ShippingMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Price breakdown preview; nothing is reserved or charged")
public record CheckoutQuoteResponse(
        @Schema(description = "Priced lines") List<CheckoutLineResponse> lines,
        @Schema(description = "Sum of lines", example = "4999.80") BigDecimal subtotal,
        @Schema(description = "Coupon discount", example = "250.00") BigDecimal discount,
        @Schema(description = "Shipping cost", example = "0.00") BigDecimal shippingCost,
        @Schema(description = "Taxes", example = "0.00") BigDecimal tax,
        @Schema(description = "Grand total", example = "4749.80") BigDecimal total,
        @Schema(description = "Currency", example = "BRL") String currency,
        @Schema(description = "Chosen shipping method", example = "STANDARD") ShippingMethod shippingMethod,
        @Schema(description = "Applied coupon", example = "WELCOME10") String couponCode
) {
}
