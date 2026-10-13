package com.marketflow.api.application.checkout.dto;

import com.marketflow.api.application.order.dto.ShippingAddressDto;
import com.marketflow.api.application.pricing.shipping.ShippingMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Checkout of the authenticated user's active cart")
public record CheckoutRequest(
        @Schema(description = "Delivery address") @NotNull @Valid ShippingAddressDto shippingAddress,
        @Schema(description = "Shipping method (Strategy)", example = "STANDARD") @NotNull ShippingMethod shippingMethod,
        @Schema(description = "Optional coupon code", example = "WELCOME10", nullable = true)
        @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_-]*$") String couponCode
) {
}
