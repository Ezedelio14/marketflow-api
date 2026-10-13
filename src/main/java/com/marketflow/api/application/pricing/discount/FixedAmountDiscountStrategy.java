package com.marketflow.api.application.pricing.discount;

import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Flat amount off, never more than the subtotal. */
@Component
public class FixedAmountDiscountStrategy implements DiscountStrategy {

    @Override
    public CouponType type() {
        return CouponType.FIXED;
    }

    @Override
    public BigDecimal calculate(Coupon coupon, BigDecimal subtotal) {
        return coupon.getValue().min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
