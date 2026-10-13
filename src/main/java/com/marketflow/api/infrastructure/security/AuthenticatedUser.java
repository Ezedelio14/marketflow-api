package com.marketflow.api.infrastructure.security;

import com.marketflow.api.application.common.Actor;
import com.marketflow.api.domain.user.UserRole;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Principal rebuilt from JWT claims on every request (stateless: no database lookup).
 */
public record AuthenticatedUser(UUID id, String email, UserRole role, String tokenId, Instant tokenExpiresAt)
        implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }

    public Actor toActor() {
        return new Actor(id, email, role);
    }
}
