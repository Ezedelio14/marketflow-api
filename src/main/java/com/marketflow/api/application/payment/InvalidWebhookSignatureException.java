package com.marketflow.api.application.payment;

import com.marketflow.api.domain.common.exception.DomainException;

public class InvalidWebhookSignatureException extends DomainException {

    public InvalidWebhookSignatureException(String message) {
        super("INVALID_WEBHOOK_SIGNATURE", message);
    }
}
