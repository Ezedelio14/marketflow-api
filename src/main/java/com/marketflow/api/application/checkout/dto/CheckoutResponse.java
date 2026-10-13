package com.marketflow.api.application.checkout.dto;

import com.marketflow.api.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Result of a successful checkout: the order is PENDING_PAYMENT and stock is reserved until reservationExpiresAt")
public record CheckoutResponse(
        @Schema(description = "Order id") UUID orderId,
        @Schema(description = "Order number", example = "MF-261013-7Q2K9XPA") String orderNumber,
        @Schema(description = "Order status", example = "PENDING_PAYMENT") OrderStatus status,
        @Schema(description = "Sum of lines", example = "4999.80") BigDecimal subtotal,
        @Schema(description = "Coupon discount", example = "250.00") BigDecimal discount,
        @Schema(description = "Shipping cost", example = "0.00") BigDecimal shippingCost,
        @Schema(description = "Taxes", example = "0.00") BigDecimal tax,
        @Schema(description = "Amount to be charged", example = "4749.80") BigDecimal total,
        @Schema(description = "Currency", example = "BRL") String currency,
        @Schema(description = "Payment data for the client-side SDK") PaymentInfo payment,
        @Schema(description = "Stock is held until this instant; unpaid orders are cancelled afterwards")
        Instant reservationExpiresAt
) {
    @Schema(description = "Payment initialised at the provider")
    public record PaymentInfo(
            @Schema(description = "Internal payment id") UUID paymentId,
            @Schema(description = "Provider", example = "stripe") String provider,
            @Schema(description = "Provider PaymentIntent id", example = "pi_3PqXyZ2eZvKYlo2C0abc1234") String providerPaymentId,
            @Schema(description = "Client secret for Stripe.js / mobile SDK confirmation", example = "pi_3PqXyZ_secret_abc") String clientSecret,
            @Schema(description = "Provider status", example = "requires_payment_method") String status
    ) {
    }
}
