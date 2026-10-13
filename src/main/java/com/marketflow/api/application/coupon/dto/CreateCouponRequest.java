package com.marketflow.api.application.coupon.dto;

import com.marketflow.api.domain.coupon.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Coupon creation payload")
public record CreateCouponRequest(
        @Schema(description = "Unique code (case-insensitive)", example = "WELCOME10") @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code,
        @Schema(description = "Discount strategy", example = "PERCENTAGE") @NotNull CouponType type,
        @Schema(description = "Percentage (0-100] or fixed amount", example = "10") @NotNull @DecimalMin("0.01") BigDecimal value,
        @Schema(description = "Minimum subtotal to apply", example = "100.00", nullable = true) @DecimalMin("0.00") BigDecimal minimumAmount,
        @Schema(description = "Cap for percentage discounts", example = "50.00", nullable = true) @DecimalMin("0.01") BigDecimal maximumDiscount,
        @Schema(description = "Maximum number of uses", example = "1000", nullable = true) @Min(1) Integer usageLimit,
        @Schema(description = "Start of validity (defaults to now)") Instant startsAt,
        @Schema(description = "End of validity", nullable = true) Instant expiresAt
) {
}
