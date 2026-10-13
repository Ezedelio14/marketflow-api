package com.marketflow.api.application.pricing.shipping;

import com.marketflow.api.domain.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy context: picks the {@link ShippingStrategy} registered for the requested method.
 */
@Component
public class ShippingCalculator {

    private final Map<ShippingMethod, ShippingStrategy> strategies = new EnumMap<>(ShippingMethod.class);

    public ShippingCalculator(List<ShippingStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.method(), s));
    }

    public BigDecimal calculate(ShippingMethod method, ShippingQuoteContext context) {
        ShippingStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new BusinessRuleException("SHIPPING_METHOD_UNAVAILABLE", "Shipping method not available: " + method);
        }
        return strategy.calculate(context);
    }
}
