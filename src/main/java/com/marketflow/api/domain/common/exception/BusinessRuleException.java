package com.marketflow.api.domain.common.exception;

/**
 * Generic business rule violation (HTTP 422).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
