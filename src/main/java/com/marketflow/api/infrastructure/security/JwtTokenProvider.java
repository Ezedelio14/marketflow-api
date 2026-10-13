package com.marketflow.api.infrastructure.security;

import com.marketflow.api.application.auth.TokenProvider;
import com.marketflow.api.domain.user.User;
import com.marketflow.api.domain.user.UserRole;
import com.marketflow.api.config.MarketflowProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * HS256-signed access tokens. Claims: {@code sub} = user id, {@code email}, {@code role}, {@code jti}.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey key;
    private final MarketflowProperties.Jwt config;
    private final Clock clock;

    public JwtTokenProvider(MarketflowProperties properties, Clock clock) {
        this.config = properties.security().jwt();
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.secret()));
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(config.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(tokenId)
                .issuer(config.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(config.issuer())
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    UserRole.valueOf(claims.get("role", String.class)),
                    claims.getId(),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
