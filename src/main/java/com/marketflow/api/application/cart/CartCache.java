package com.marketflow.api.application.cart;

import com.marketflow.api.application.cart.dto.CartResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for the temporary cart cache (Redis). The database stays the source of truth.
 */
public interface CartCache {

    Optional<CartResponse> get(UUID userId);

    void put(CartResponse cart);

    void evict(UUID userId);
}
