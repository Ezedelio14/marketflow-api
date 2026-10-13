package com.marketflow.api.application.coupon.dto;

import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponStatus;
import com.marketflow.api.domain.coupon.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Coupon")
public record CouponResponse(
        @Schema(description = "Coupon id") UUID id,
        @Schema(description = "Code", example = "WELCOME10") String code,
        @Schema(description = "Type", example = "PERCENTAGE") CouponType type,
        @Schema(description = "Value", example = "10") BigDecimal value,
        @Schema(description = "Minimum subtotal") BigDecimal minimumAmount,
        @Schema(description = "Maximum discount") BigDecimal maximumDiscount,
        @Schema(description = "Usage limit") Integer usageLimit,
        @Schema(description = "Times used", example = "42") int usedCount,
        @Schema(description = "Start of validity") Instant startsAt,
        @Schema(description = "End of validity") Instant expiresAt,
        @Schema(description = "Status", example = "ACTIVE") CouponStatus status
) {
    public static CouponResponse from(Coupon c) {
        return new CouponResponse(c.getId(), c.getCode(), c.getType(), c.getValue(), c.getMinimumAmount(),
                c.getMaximumDiscount(), c.getUsageLimit(), c.getUsedCount(), c.getStartsAt(), c.getExpiresAt(), c.getStatus());
    }
}
