package com.marketflow.api.application.checkout;

import com.marketflow.api.application.pricing.shipping.ShippingMethod;
import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.catalog.ProductVariant;
import lombok.Builder;
import lombok.Singular;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable pricing snapshot built step by step by {@link CheckoutPricing} (Builder pattern).
 */
@Builder
public record CheckoutQuote(
        @Singular List<Line> lines,
        BigDecimal subtotal,
        BigDecimal totalWeight,
        BigDecimal discount,
        BigDecimal shippingCost,
        BigDecimal tax,
        BigDecimal total,
        String currency,
        ShippingMethod shippingMethod,
        Coupon coupon
) {
    public record Line(ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
