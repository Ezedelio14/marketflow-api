package com.marketflow.api.application.pricing.shipping;

import java.math.BigDecimal;

/**
 * Strategy pattern: one implementation per {@link ShippingMethod}. New carriers are added as new beans without
 * touching the checkout flow.
 */
public interface ShippingStrategy {

    ShippingMethod method();

    BigDecimal calculate(ShippingQuoteContext context);
}
