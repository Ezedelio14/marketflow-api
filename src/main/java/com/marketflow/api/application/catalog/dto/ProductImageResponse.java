package com.marketflow.api.application.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Product image")
public record ProductImageResponse(
        @Schema(description = "Image id") UUID id,
        @Schema(description = "Public URL") String url,
        @Schema(description = "Display position", example = "0") int position,
        @Schema(description = "Alternative text") String altText
) {
}
