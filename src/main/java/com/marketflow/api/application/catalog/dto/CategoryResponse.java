package com.marketflow.api.application.catalog.dto;

import com.marketflow.api.domain.catalog.Category;
import com.marketflow.api.domain.catalog.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Product category")
public record CategoryResponse(
        @Schema(description = "Category id") UUID id,
        @Schema(description = "Parent category id", nullable = true) UUID parentId,
        @Schema(description = "Display name", example = "Smartphones") String name,
        @Schema(description = "URL slug", example = "smartphones") String slug,
        @Schema(description = "Description") String description,
        @Schema(description = "Status", example = "ACTIVE") CategoryStatus status
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getParent() == null ? null : c.getParent().getId(), c.getName(),
                c.getSlug(), c.getDescription(), c.getStatus());
    }
}
