package com.marketflow.api.application.payment;

/**
 * Output port that authenticates and translates a raw provider webhook into a {@link PaymentWebhookEvent}.
 * Implementations must reject payloads whose signature does not verify.
 */
public interface PaymentWebhookParser {

    PaymentWebhookEvent parse(String payload, String signatureHeader);
}
