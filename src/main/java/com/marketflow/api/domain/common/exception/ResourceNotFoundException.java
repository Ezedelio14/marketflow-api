package com.marketflow.api.domain.common.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", "%s not found: %s".formatted(resource, id));
    }
}
