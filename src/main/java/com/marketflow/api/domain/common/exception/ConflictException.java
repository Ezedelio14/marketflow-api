package com.marketflow.api.domain.common.exception;

/**
 * State conflict, e.g. duplicated unique value or concurrent modification (HTTP 409).
 */
public class ConflictException extends DomainException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
