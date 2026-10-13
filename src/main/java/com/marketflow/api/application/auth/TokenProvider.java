package com.marketflow.api.application.auth;

import com.marketflow.api.domain.user.User;

import java.time.Instant;

/**
 * Output port for access-token issuing (implemented with JWT in the infrastructure layer).
 */
public interface TokenProvider {

    IssuedToken issue(User user);

    record IssuedToken(String value, String tokenId, Instant expiresAt) {
    }
}
