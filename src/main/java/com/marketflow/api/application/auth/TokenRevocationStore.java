package com.marketflow.api.application.auth;

import java.time.Instant;

/**
 * Output port for revoked tokens (logout). Backed by Redis with a TTL equal to the token's remaining lifetime.
 */
public interface TokenRevocationStore {

    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}
