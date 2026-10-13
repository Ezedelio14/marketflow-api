package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Brand creation payload")
public record CreateBrandRequest(
        @Schema(description = "Brand name", example = "Acme") @NotBlank @Size(max = 255) String name,
        @Schema(description = "URL slug; generated when omitted", example = "acme") @Size(max = 255) String slug,
        @Schema(description = "Description", example = "Quality electronics since 1999") @Size(max = 255) String description,
        @Schema(description = "Logo URL", example = "https://cdn.marketflow.dev/brands/acme.png") @URL @Size(max = 255) String logoUrl
) {
}
