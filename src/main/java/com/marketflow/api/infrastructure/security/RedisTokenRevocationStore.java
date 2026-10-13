package com.marketflow.api.infrastructure.security;

import com.marketflow.api.application.auth.TokenRevocationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Session cache in Redis: a deny-list of logged-out token ids, each kept only until the token would expire anyway.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenRevocationStore implements TokenRevocationStore {

    private static final String PREFIX = "auth:revoked:";

    private final StringRedisTemplate redis;

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(PREFIX + tokenId, "1", ttl);
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + tokenId));
        } catch (RuntimeException e) {
            log.warn("Revocation store unavailable: {}", e.getMessage());
            return false;
        }
    }
}
