package com.marketflow.api.application.catalog.dto;

import com.marketflow.api.domain.catalog.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact product row for back-office listings")
public record ProductSummaryResponse(
        @Schema(description = "Product id") UUID id,
        @Schema(description = "Seller id") UUID sellerId,
        @Schema(description = "Category id") UUID categoryId,
        @Schema(description = "Brand id") UUID brandId,
        @Schema(description = "Product name", example = "Phone X1") String name,
        @Schema(description = "URL slug", example = "phone-x1") String slug,
        @Schema(description = "Status", example = "DRAFT") ProductStatus status,
        @Schema(description = "Last update instant") Instant updatedAt
) {
}
