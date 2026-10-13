package com.marketflow.api.application.pricing;

import com.marketflow.api.application.pricing.discount.DiscountCalculator;
import com.marketflow.api.application.pricing.discount.FixedAmountDiscountStrategy;
import com.marketflow.api.application.pricing.discount.PercentageDiscountStrategy;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountCalculatorTest {

    private final DiscountCalculator calculator = new DiscountCalculator(
            List.of(new PercentageDiscountStrategy(), new FixedAmountDiscountStrategy()));
    private final Instant now = Instant.now();

    @Test
    void percentageIsCappedByMaximumDiscount() {
        Coupon coupon = Coupon.create("TEN", CouponType.PERCENTAGE, new BigDecimal("10"), null, new BigDecimal("30.00"), null, now.minusSeconds(60), null);
        assertThat(calculator.calculate(coupon, new BigDecimal("200.00"), now)).isEqualByComparingTo("20.00");
        assertThat(calculator.calculate(coupon, new BigDecimal("1000.00"), now)).isEqualByComparingTo("30.00");
    }

    @Test
    void fixedNeverExceedsSubtotal() {
        Coupon coupon = Coupon.create("FIFTY", CouponType.FIXED, new BigDecimal("50.00"), null, null, null, now.minusSeconds(60), null);
        assertThat(calculator.calculate(coupon, new BigDecimal("30.00"), now)).isEqualByComparingTo("30.00");
    }

    @Test
    void rejectsSubtotalBelowMinimum() {
        Coupon coupon = Coupon.create("MIN", CouponType.FIXED, new BigDecimal("10.00"), new BigDecimal("100.00"), null, null, now.minusSeconds(60), null);
        assertThatThrownBy(() -> calculator.calculate(coupon, new BigDecimal("99.99"), now))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("at least");
    }

    @Test
    void rejectsExpiredCoupon() {
        Coupon coupon = Coupon.create("OLD", CouponType.FIXED, new BigDecimal("10.00"), null, null, null,
                now.minusSeconds(3600), now.minusSeconds(60));
        assertThatThrownBy(() -> calculator.calculate(coupon, new BigDecimal("50.00"), now)).isInstanceOf(BusinessRuleException.class);
    }
}
