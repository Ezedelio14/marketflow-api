package com.marketflow.api.domain.common.exception;

import java.util.UUID;

public class InsufficientStockException extends DomainException {

    public InsufficientStockException(UUID variantId, int requested, int available) {
        super("INSUFFICIENT_STOCK",
                "Insufficient stock for variant %s: requested %d, available %d".formatted(variantId, requested, available));
    }
}
