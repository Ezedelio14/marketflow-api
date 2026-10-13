package com.marketflow.api.application.payment;

/**
 * Provider-agnostic view of a payment webhook.
 */
public record PaymentWebhookEvent(
        String eventId,
        String rawType,
        Kind kind,
        String providerPaymentId,
        String failureReason
) {
    public enum Kind {
        PAYMENT_SUCCEEDED, PAYMENT_FAILED, PAYMENT_CANCELED, REFUNDED, IGNORED
    }
}
