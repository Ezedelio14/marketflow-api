package com.marketflow.api.application.pricing.discount;

import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponType;

import java.math.BigDecimal;

/**
 * Strategy pattern: how a coupon type turns an order subtotal into a discount amount.
 */
public interface DiscountStrategy {

    CouponType type();

    BigDecimal calculate(Coupon coupon, BigDecimal subtotal);
}
