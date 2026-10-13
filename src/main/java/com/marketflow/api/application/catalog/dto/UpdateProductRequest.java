package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Partial product update; null fields are left unchanged")
public record UpdateProductRequest(
        @Schema(description = "New category id", nullable = true) UUID categoryId,
        @Schema(description = "New brand id", nullable = true) UUID brandId,
        @Schema(description = "New name", example = "Phone X1 Pro", nullable = true) @Size(max = 255) String name,
        @Schema(description = "New description", nullable = true) @Size(max = 10000) String description
) {
}
