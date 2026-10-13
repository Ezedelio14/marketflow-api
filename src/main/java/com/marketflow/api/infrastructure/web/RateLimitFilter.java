package com.marketflow.api.infrastructure.web;

import com.marketflow.api.config.MarketflowProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis (atomic INCR + EXPIRE via Lua), shared by all API instances.
 * Limits are per client IP and per endpoint group; auth and checkout get stricter quotas. Fails open if Redis is
 * unavailable so an outage of the cache never takes the API down.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);
    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redis;
    private final MarketflowProperties properties;
    private final ProblemDetailsWriter problemWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.rateLimit().enabled() || !path.startsWith("/api/") || path.startsWith("/api/v1/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String group = group(request.getRequestURI());
        int limit = limitFor(group);
        long window = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        String key = "rl:%s:%s:%d".formatted(group, clientIp(request), window);

        Long count;
        try {
            count = redis.execute(INCREMENT, List.of(key), String.valueOf(WINDOW_SECONDS));
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }
        long used = count == null ? 0 : count;
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));
        if (used > limit) {
            long retryAfter = WINDOW_SECONDS - (Instant.now().getEpochSecond() % WINDOW_SECONDS);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            problemWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                    "Too many requests, retry in %d seconds".formatted(retryAfter));
            return;
        }
        chain.doFilter(request, response);
    }

    private static String group(String path) {
        if (path.startsWith("/api/v1/auth/")) return "auth";
        if (path.startsWith("/api/v1/checkout")) return "checkout";
        return "default";
    }

    private int limitFor(String group) {
        MarketflowProperties.RateLimit cfg = properties.rateLimit();
        return switch (group) {
            case "auth" -> cfg.authPerMinute();
            case "checkout" -> cfg.checkoutPerMinute();
            default -> cfg.defaultPerMinute();
        };
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
