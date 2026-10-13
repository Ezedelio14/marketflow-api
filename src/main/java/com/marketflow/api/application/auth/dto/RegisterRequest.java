package com.marketflow.api.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer sign-up payload")
public record RegisterRequest(
        @Schema(description = "Unique e-mail used as login", example = "jane.doe@example.com")
        @NotBlank @Email @Size(max = 255)
        String email,

        @Schema(description = "Password: 8-72 chars with at least one letter and one digit", example = "S3cret!pass")
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain letters and digits")
        String password
) {
}
