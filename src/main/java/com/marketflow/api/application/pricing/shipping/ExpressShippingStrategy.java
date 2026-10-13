package com.marketflow.api.application.pricing.shipping;

import com.marketflow.api.config.MarketflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Priority delivery: higher base and per-kg fees, never free. */
@Component
@RequiredArgsConstructor
public class ExpressShippingStrategy implements ShippingStrategy {

    private final MarketflowProperties properties;

    @Override
    public ShippingMethod method() {
        return ShippingMethod.EXPRESS;
    }

    @Override
    public BigDecimal calculate(ShippingQuoteContext context) {
        MarketflowProperties.Shipping cfg = properties.shipping();
        return cfg.expressBase()
                .add(cfg.expressPerKg().multiply(context.totalWeightKg()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
