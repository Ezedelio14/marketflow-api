package com.marketflow.api.application.order.dto;

import com.marketflow.api.domain.order.OrderStatus;
import com.marketflow.api.domain.order.state.OrderAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Full order view")
public record OrderResponse(
        @Schema(description = "Order id") UUID id,
        @Schema(description = "Human-friendly unique number", example = "MF-261013-7Q2K9XPA") String orderNumber,
        @Schema(description = "Customer id") UUID userId,
        @Schema(description = "Current lifecycle status", example = "PAID") OrderStatus status,
        @Schema(description = "Sum of items", example = "4999.80") BigDecimal subtotal,
        @Schema(description = "Coupon discount", example = "250.00") BigDecimal discount,
        @Schema(description = "Shipping cost", example = "0.00") BigDecimal shippingCost,
        @Schema(description = "Taxes", example = "0.00") BigDecimal tax,
        @Schema(description = "Amount charged", example = "4749.80") BigDecimal total,
        @Schema(description = "ISO 4217 currency", example = "BRL") String currency,
        @Schema(description = "Applied coupon code", example = "WELCOME10") String couponCode,
        @Schema(description = "Delivery address") ShippingAddressDto shippingAddress,
        @Schema(description = "Order lines") List<OrderItemResponse> items,
        @Schema(description = "State transition history") List<OrderStatusHistoryResponse> statusHistory,
        @Schema(description = "Actions accepted by the state machine from the current status", example = "[\"START_PROCESSING\",\"REQUEST_REFUND\"]")
        Set<OrderAction> allowedActions,
        @Schema(description = "Creation instant") Instant createdAt,
        @Schema(description = "Last update instant") Instant updatedAt
) {
}
