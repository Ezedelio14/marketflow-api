package com.marketflow.api.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketflow.api.config.MarketflowProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Redis-backed idempotency for {@link Idempotent} endpoints.
 * <ol>
 *   <li>No stored entry: a short-lived {@code IN_PROGRESS} lock is taken with {@code SET NX} and the request runs.</li>
 *   <li>Entry is {@code IN_PROGRESS}: a concurrent duplicate → 409.</li>
 *   <li>Entry holds a response: it is replayed verbatim with {@code Idempotent-Replayed: true}.</li>
 * </ol>
 * Keys are scoped by user, method and path so two users can never collide. 5xx responses release the lock so the
 * client can retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "Idempotency-Key";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String ATTR_KEY = IdempotencyInterceptor.class.getName() + ".key";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MarketflowProperties properties;
    private final ProblemDetailsWriter problemWriter;

    record StoredResponse(int status, String contentType, String body) {
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method) || !method.hasMethodAnnotation(Idempotent.class)) {
            return true;
        }
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            if (method.getMethodAnnotation(Idempotent.class).required()) {
                problemWriter.write(request, response, HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                        "Header '" + HEADER + "' is required for this operation");
                return false;
            }
            return true;
        }
        if (key.length() > 255) {
            problemWriter.write(request, response, HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_INVALID", "Idempotency key is too long");
            return false;
        }

        String redisKey = "idem:" + sha256(principal() + "|" + request.getMethod() + "|" + request.getRequestURI() + "|" + key);
        String stored = redis.opsForValue().get(redisKey);
        if (stored == null) {
            Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, properties.idempotency().lockTtl());
            if (Boolean.TRUE.equals(acquired)) {
                request.setAttribute(ATTR_KEY, redisKey);
                return true;
            }
            stored = redis.opsForValue().get(redisKey);
        }
        if (stored == null || IN_PROGRESS.equals(stored)) {
            problemWriter.write(request, response, HttpStatus.CONFLICT, "IDEMPOTENT_REQUEST_IN_PROGRESS",
                    "A request with this idempotency key is still being processed");
            return false;
        }
        StoredResponse replay = objectMapper.readValue(stored, StoredResponse.class);
        response.setStatus(replay.status());
        if (replay.contentType() != null) {
            response.setContentType(replay.contentType());
        }
        response.setHeader("Idempotent-Replayed", "true");
        response.getOutputStream().write(replay.body().getBytes(StandardCharsets.UTF_8));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String redisKey = (String) request.getAttribute(ATTR_KEY);
        if (redisKey == null) {
            return;
        }
        try {
            int status = response.getStatus();
            ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
            if (ex != null || status >= 500 || wrapper == null) {
                redis.delete(redisKey);
                return;
            }
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            String json = objectMapper.writeValueAsString(new StoredResponse(status, response.getContentType(), body));
            redis.opsForValue().set(redisKey, json, properties.idempotency().ttl());
        } catch (Exception e) {
            log.warn("Could not store idempotent response: {}", e.getMessage());
            redis.delete(redisKey);
        }
    }

    private static String principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
