package com.marketflow.api.domain.common.exception;

/**
 * Failure while talking to the external payment provider (HTTP 502).
 */
public class PaymentProviderException extends DomainException {

    public PaymentProviderException(String message, Throwable cause) {
        super("PAYMENT_PROVIDER_ERROR", message);
        initCause(cause);
    }
}
