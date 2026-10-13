package com.marketflow.api.application.pricing.shipping;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Customer collects the parcel at a pickup point: no shipping fee. */
@Component
public class PickupShippingStrategy implements ShippingStrategy {

    @Override
    public ShippingMethod method() {
        return ShippingMethod.PICKUP;
    }

    @Override
    public BigDecimal calculate(ShippingQuoteContext context) {
        return BigDecimal.ZERO.setScale(2);
    }
}
