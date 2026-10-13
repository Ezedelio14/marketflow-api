package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Product creation payload. The product starts as DRAFT.")
public record CreateProductRequest(
        @Schema(description = "Category id") @NotNull UUID categoryId,
        @Schema(description = "Brand id", nullable = true) UUID brandId,
        @Schema(description = "Product name", example = "Phone X1") @NotBlank @Size(max = 255) String name,
        @Schema(description = "Rich description", example = "6.1\" OLED display, 48MP camera") @Size(max = 10000) String description,
        @ArraySchema(arraySchema = @Schema(description = "Initial variants (optional)"))
        @Valid @Size(max = 100) List<CreateVariantRequest> variants
) {
}
