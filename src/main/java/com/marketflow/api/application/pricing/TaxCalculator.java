package com.marketflow.api.application.pricing;

import com.marketflow.api.config.MarketflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Flat-rate tax over the discounted subtotal (pluggable for a tax provider later). */
@Component
@RequiredArgsConstructor
public class TaxCalculator {

    private final MarketflowProperties properties;

    public BigDecimal calculate(BigDecimal taxableAmount) {
        BigDecimal rate = properties.tax().rate() == null ? BigDecimal.ZERO : properties.tax().rate();
        return taxableAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
