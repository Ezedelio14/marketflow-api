package com.marketflow.api.domain.common.exception;

/**
 * Authenticated user is not allowed to act on the given resource (HTTP 403).
 */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super("FORBIDDEN_OPERATION", message);
    }
}
