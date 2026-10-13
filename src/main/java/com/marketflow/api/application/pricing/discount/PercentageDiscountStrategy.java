package com.marketflow.api.application.pricing.discount;

import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** {@code value}% of the subtotal, capped by {@code maximum_discount} when configured. */
@Component
public class PercentageDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public CouponType type() {
        return CouponType.PERCENTAGE;
    }

    @Override
    public BigDecimal calculate(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = subtotal.multiply(coupon.getValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        if (coupon.getMaximumDiscount() != null) {
            discount = discount.min(coupon.getMaximumDiscount());
        }
        return discount.min(subtotal);
    }
}
