package com.marketflow.api.application.pricing.shipping;

import com.marketflow.api.config.MarketflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Base fee + per-kg fee, free above the configured threshold. */
@Component
@RequiredArgsConstructor
public class StandardShippingStrategy implements ShippingStrategy {

    private final MarketflowProperties properties;

    @Override
    public ShippingMethod method() {
        return ShippingMethod.STANDARD;
    }

    @Override
    public BigDecimal calculate(ShippingQuoteContext context) {
        MarketflowProperties.Shipping cfg = properties.shipping();
        if (context.subtotal().compareTo(cfg.freeShippingThreshold()) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return cfg.standardBase()
                .add(cfg.standardPerKg().multiply(context.totalWeightKg()))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
