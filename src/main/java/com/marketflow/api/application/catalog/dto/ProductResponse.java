package com.marketflow.api.application.catalog.dto;

import com.marketflow.api.domain.catalog.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Product with variants and images")
public record ProductResponse(
        @Schema(description = "Product id") UUID id,
        @Schema(description = "Seller id") UUID sellerId,
        @Schema(description = "Seller store name", example = "Tech Store") String sellerName,
        @Schema(description = "Category id") UUID categoryId,
        @Schema(description = "Category name", example = "Smartphones") String categoryName,
        @Schema(description = "Brand id") UUID brandId,
        @Schema(description = "Brand name", example = "Acme") String brandName,
        @Schema(description = "Product name", example = "Phone X1") String name,
        @Schema(description = "URL slug", example = "phone-x1") String slug,
        @Schema(description = "Description") String description,
        @Schema(description = "Lifecycle status", example = "PUBLISHED") ProductStatus status,
        @Schema(description = "Variants") List<ProductVariantResponse> variants,
        @Schema(description = "Images ordered by position") List<ProductImageResponse> images,
        @Schema(description = "Creation instant") Instant createdAt,
        @Schema(description = "Last update instant") Instant updatedAt
) {
}
