package com.marketflow.api.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for authentication")
public record LoginRequest(
        @Schema(description = "Account e-mail", example = "jane.doe@example.com") @NotBlank @Email String email,
        @Schema(description = "Account password", example = "S3cret!pass") @NotBlank String password
) {
}
