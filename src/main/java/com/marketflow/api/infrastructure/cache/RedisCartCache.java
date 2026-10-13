package com.marketflow.api.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.api.application.cart.CartCache;
import com.marketflow.api.application.cart.dto.CartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis repository for the temporary cart snapshot (JSON, 7-day sliding TTL). Any Redis failure degrades to the
 * database instead of failing the request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCartCache implements CartCache {

    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<CartResponse> get(UUID userId) {
        try {
            String json = redis.opsForValue().getAndExpire(key(userId), TTL);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, CartResponse.class));
        } catch (Exception e) {
            log.warn("Cart cache read failed for {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(CartResponse cart) {
        try {
            redis.opsForValue().set(key(cart.userId()), objectMapper.writeValueAsString(cart), TTL);
        } catch (Exception e) {
            log.warn("Cart cache write failed for {}: {}", cart.userId(), e.getMessage());
        }
    }

    @Override
    public void evict(UUID userId) {
        try {
            redis.delete(key(userId));
        } catch (RuntimeException e) {
            log.warn("Cart cache eviction failed for {}: {}", userId, e.getMessage());
        }
    }

    private static String key(UUID userId) {
        return "cart:" + userId;
    }
}
