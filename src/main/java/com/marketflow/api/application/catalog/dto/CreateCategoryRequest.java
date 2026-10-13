package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Category creation payload")
public record CreateCategoryRequest(
        @Schema(description = "Parent category id for sub-categories", nullable = true) UUID parentId,
        @Schema(description = "Display name", example = "Smartphones") @NotBlank @Size(max = 255) String name,
        @Schema(description = "URL slug; generated from the name when omitted", example = "smartphones") @Size(max = 255) String slug,
        @Schema(description = "Short description", example = "Phones and accessories") @Size(max = 255) String description
) {
}
