package com.marketflow.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Strongly typed application settings bound from the {@code marketflow.*} namespace.
 */
@Validated
@ConfigurationProperties(prefix = "marketflow")
public record MarketflowProperties(
        String currency,
        Security security,
        Stripe stripe,
        Checkout checkout,
        Tax tax,
        Shipping shipping,
        RateLimit rateLimit,
        Idempotency idempotency,
        Bootstrap bootstrap
) {

    public record Security(Jwt jwt) {
    }

    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {
    }

    public record Stripe(String secretKey, String webhookSecret) {
    }

    public record Checkout(Duration reservationTtl, int optimisticAttempts, int maxAttempts, Duration expirationJobDelay) {
    }

    public record Tax(BigDecimal rate) {
    }

    public record Shipping(BigDecimal standardBase, BigDecimal standardPerKg, BigDecimal expressBase,
                           BigDecimal expressPerKg, BigDecimal freeShippingThreshold) {
    }

    public record RateLimit(boolean enabled, int defaultPerMinute, int authPerMinute, int checkoutPerMinute) {
    }

    public record Idempotency(Duration ttl, Duration lockTtl) {
    }

    public record Bootstrap(String adminEmail, String adminPassword) {
    }
}
