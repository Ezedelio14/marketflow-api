package com.marketflow.api.application.auth.dto;

import com.marketflow.api.domain.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Issued access token")
public record AuthResponse(
        @Schema(description = "JWT to send as 'Authorization: Bearer <token>'") String accessToken,
        @Schema(description = "Token type", example = "Bearer") String tokenType,
        @Schema(description = "Expiration instant (UTC)") Instant expiresAt,
        @Schema(description = "Authenticated user id") UUID userId,
        @Schema(description = "Authenticated user e-mail", example = "jane.doe@example.com") String email,
        @Schema(description = "Role used for RBAC", example = "CUSTOMER") UserRole role
) {
}
