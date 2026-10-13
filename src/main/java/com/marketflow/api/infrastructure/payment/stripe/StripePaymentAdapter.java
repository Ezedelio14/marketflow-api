package com.marketflow.api.infrastructure.payment.stripe;

import com.marketflow.api.application.payment.PaymentGateway;
import com.marketflow.api.domain.common.exception.PaymentProviderException;
import com.marketflow.api.config.MarketflowProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter pattern: translates the provider-agnostic {@link PaymentGateway} port into Stripe SDK calls.
 * Every mutating call carries a Stripe idempotency key so network retries never double-charge or double-refund.
 */
@Slf4j
@Component
public class StripePaymentAdapter implements PaymentGateway {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of("BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
            "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private final String secretKey;

    public StripePaymentAdapter(MarketflowProperties properties) {
        this.secretKey = properties.stripe().secretKey();
    }

    @Override
    public String providerName() {
        return "stripe";
    }

    @Override
    public PaymentIntentResult createPaymentIntent(PaymentIntentCommand command) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(command.amount(), command.currency()))
                .setCurrency(command.currency().toLowerCase(Locale.ROOT))
                .setDescription("MarketFlow order " + command.orderNumber())
                .setReceiptEmail(command.customerEmail())
                .putAllMetadata(command.metadata())
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params, options(command.idempotencyKey()));
            log.info("Stripe PaymentIntent {} created for order {}", intent.getId(), command.orderNumber());
            return new PaymentIntentResult(intent.getId(), intent.getClientSecret(), intent.getStatus());
        } catch (StripeException e) {
            throw new PaymentProviderException("Stripe rejected PaymentIntent creation: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelPaymentIntent(String providerPaymentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerPaymentId, options(null));
            if (!"canceled".equals(intent.getStatus()) && !"succeeded".equals(intent.getStatus())) {
                intent.cancel(PaymentIntentCancelParams.builder()
                        .setCancellationReason(PaymentIntentCancelParams.CancellationReason.ABANDONED)
                        .build(), options("cancel-" + providerPaymentId));
            }
        } catch (StripeException e) {
            throw new PaymentProviderException("Stripe rejected PaymentIntent cancellation: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refund(String providerPaymentId, BigDecimal amount, String currency, String idempotencyKey) {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(providerPaymentId)
                .setAmount(toMinorUnits(amount, currency))
                .build();
        try {
            Refund refund = Refund.create(params, options(idempotencyKey));
            return new RefundResult(refund.getId(), refund.getStatus());
        } catch (StripeException e) {
            throw new PaymentProviderException("Stripe rejected refund: " + e.getMessage(), e);
        }
    }

    private RequestOptions options(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(secretKey);
        if (idempotencyKey != null) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    static long toMinorUnits(BigDecimal amount, String currency) {
        int exponent = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT)) ? 0 : 2;
        return amount.movePointRight(exponent).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}
