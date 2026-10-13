package com.marketflow.api.domain.common.exception;

import lombok.Getter;

/**
 * Base class for business rule violations raised by the domain layer.
 * Mapped to RFC 7807 Problem Details by the global exception handler.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
