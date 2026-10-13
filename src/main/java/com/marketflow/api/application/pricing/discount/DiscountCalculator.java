package com.marketflow.api.application.pricing.discount;

import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy context for coupon discounts. Validates the coupon's business rules, then delegates the math.
 */
@Component
public class DiscountCalculator {

    private final Map<CouponType, DiscountStrategy> strategies = new EnumMap<>(CouponType.class);

    public DiscountCalculator(List<DiscountStrategy> strategies) {
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public BigDecimal calculate(Coupon coupon, BigDecimal subtotal, Instant now) {
        coupon.ensureApplicable(subtotal, now);
        return strategies.get(coupon.getType()).calculate(coupon, subtotal);
    }
}
