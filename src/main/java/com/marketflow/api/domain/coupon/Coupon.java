package com.marketflow.api.domain.coupon;

import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CouponType type;

    @Column(name = "value", nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(name = "minimum_amount", precision = 19, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_discount", precision = 19, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status;

    public static Coupon create(String code, CouponType type, BigDecimal value, BigDecimal minimumAmount,
                                BigDecimal maximumDiscount, Integer usageLimit, Instant startsAt, Instant expiresAt) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException("INVALID_COUPON", "Coupon value must be positive");
        }
        if (type == CouponType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("INVALID_COUPON", "Percentage coupons cannot exceed 100%");
        }
        Coupon coupon = new Coupon();
        coupon.code = code.trim().toUpperCase();
        coupon.type = type;
        coupon.value = value;
        coupon.minimumAmount = minimumAmount;
        coupon.maximumDiscount = maximumDiscount;
        coupon.usageLimit = usageLimit;
        coupon.usedCount = 0;
        coupon.startsAt = startsAt == null ? Instant.now() : startsAt;
        coupon.expiresAt = expiresAt;
        coupon.status = CouponStatus.ACTIVE;
        return coupon;
    }

    /** Validates every rule that makes the coupon usable for a given order subtotal. */
    public void ensureApplicable(BigDecimal subtotal, Instant now) {
        if (status != CouponStatus.ACTIVE) {
            throw new BusinessRuleException("COUPON_INACTIVE", "Coupon is not active");
        }
        if (now.isBefore(startsAt) || (expiresAt != null && now.isAfter(expiresAt))) {
            throw new BusinessRuleException("COUPON_EXPIRED", "Coupon is not valid at this time");
        }
        if (usageLimit != null && usedCount >= usageLimit) {
            throw new BusinessRuleException("COUPON_EXHAUSTED", "Coupon usage limit reached");
        }
        if (minimumAmount != null && subtotal.compareTo(minimumAmount) < 0) {
            throw new BusinessRuleException("COUPON_MINIMUM_NOT_REACHED",
                    "Order subtotal must be at least " + minimumAmount);
        }
    }

    public void deactivate() {
        status = CouponStatus.INACTIVE;
    }
}
