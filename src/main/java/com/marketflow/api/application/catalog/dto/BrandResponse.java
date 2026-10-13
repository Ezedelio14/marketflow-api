package com.marketflow.api.application.catalog.dto;

import com.marketflow.api.domain.catalog.Brand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Brand")
public record BrandResponse(
        @Schema(description = "Brand id") UUID id,
        @Schema(description = "Brand name", example = "Acme") String name,
        @Schema(description = "URL slug", example = "acme") String slug,
        @Schema(description = "Description") String description,
        @Schema(description = "Logo URL") String logoUrl
) {
    public static BrandResponse from(Brand b) {
        return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getDescription(), b.getLogoUrl());
    }
}
