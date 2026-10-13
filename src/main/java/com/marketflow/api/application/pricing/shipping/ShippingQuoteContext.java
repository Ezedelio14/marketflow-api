package com.marketflow.api.application.pricing.shipping;

import com.marketflow.api.domain.order.ShippingAddress;

import java.math.BigDecimal;

/**
 * Input of a shipping strategy: what is being shipped and where.
 */
public record ShippingQuoteContext(BigDecimal subtotal, BigDecimal totalWeightKg, ShippingAddress destination) {
}
