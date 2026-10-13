package com.marketflow.api.application.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Output port for the payment provider (Adapter pattern: implemented by {@code StripePaymentAdapter}).
 */
public interface PaymentGateway {

    String providerName();

    PaymentIntentResult createPaymentIntent(PaymentIntentCommand command);

    void cancelPaymentIntent(String providerPaymentId);

    RefundResult refund(String providerPaymentId, BigDecimal amount, String currency, String idempotencyKey);

    record PaymentIntentCommand(UUID orderId, String orderNumber, UUID userId, String customerEmail,
                                BigDecimal amount, String currency, String idempotencyKey, Map<String, String> metadata) {
    }

    record PaymentIntentResult(String providerPaymentId, String clientSecret, String status) {
    }

    record RefundResult(String refundId, String status) {
        public boolean succeeded() {
            return "succeeded".equalsIgnoreCase(status);
        }
    }
}
