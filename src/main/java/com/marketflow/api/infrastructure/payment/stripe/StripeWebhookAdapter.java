package com.marketflow.api.infrastructure.payment.stripe;

import com.marketflow.api.application.payment.InvalidWebhookSignatureException;
import com.marketflow.api.application.payment.PaymentWebhookEvent;
import com.marketflow.api.application.payment.PaymentWebhookEvent.Kind;
import com.marketflow.api.application.payment.PaymentWebhookParser;
import com.marketflow.api.config.MarketflowProperties;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Verifies the {@code Stripe-Signature} header strictly with {@link Webhook#constructEvent} (HMAC-SHA256 over the
 * raw body + timestamp tolerance) and maps the Stripe event to the provider-agnostic {@link PaymentWebhookEvent}.
 */
@Slf4j
@Component
public class StripeWebhookAdapter implements PaymentWebhookParser {

    private final String webhookSecret;

    public StripeWebhookAdapter(MarketflowProperties properties) {
        this.webhookSecret = properties.stripe().webhookSecret();
    }

    @Override
    public PaymentWebhookEvent parse(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidWebhookSignatureException("Missing Stripe-Signature header");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new InvalidWebhookSignatureException("Stripe signature verification failed");
        } catch (RuntimeException e) {
            throw new InvalidWebhookSignatureException("Malformed Stripe payload");
        }

        StripeObject object = dataObject(event);
        return switch (event.getType()) {
            case "payment_intent.succeeded" -> of(event, Kind.PAYMENT_SUCCEEDED, intentId(object), null);
            case "payment_intent.payment_failed" -> of(event, Kind.PAYMENT_FAILED, intentId(object), failureMessage(object));
            case "payment_intent.canceled" -> of(event, Kind.PAYMENT_CANCELED, intentId(object), null);
            case "charge.refunded" -> object instanceof Charge charge && Boolean.TRUE.equals(charge.getRefunded())
                    ? of(event, Kind.REFUNDED, charge.getPaymentIntent(), null)
                    : of(event, Kind.IGNORED, null, null); // partial refunds are not modelled yet
            default -> of(event, Kind.IGNORED, null, null);
        };
    }

    private static PaymentWebhookEvent of(Event event, Kind kind, String paymentIntentId, String failure) {
        return new PaymentWebhookEvent(event.getId(), event.getType(), kind, paymentIntentId, failure);
    }

    private static StripeObject dataObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return deserializer.getObject().get();
        }
        try {
            // API version of the event differs from the SDK's pinned version: deserialize best-effort.
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            log.warn("Could not deserialize Stripe event {} ({})", event.getId(), event.getType());
            return null;
        }
    }

    private static String intentId(StripeObject object) {
        return object instanceof PaymentIntent intent ? intent.getId() : null;
    }

    private static String failureMessage(StripeObject object) {
        if (object instanceof PaymentIntent intent && intent.getLastPaymentError() != null) {
            return intent.getLastPaymentError().getMessage();
        }
        return null;
    }
}
