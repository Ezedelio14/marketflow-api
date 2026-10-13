package com.marketflow.api.application.catalog.search;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product returned by the storefront search")
public record ProductSearchHit(
        @Schema(description = "Product id") UUID id,
        @Schema(description = "URL slug", example = "phone-x1") String slug,
        @Schema(description = "Product name", example = "Phone X1") String name,
        @Schema(description = "Short description") String description,
        @Schema(description = "Brand name", example = "Acme") String brandName,
        @Schema(description = "Category name", example = "Smartphones") String categoryName,
        @Schema(description = "Seller store name", example = "Tech Store") String sellerName,
        @Schema(description = "Lowest active variant price", example = "2499.90") BigDecimal minPrice,
        @Schema(description = "Highest active variant price", example = "2999.90") BigDecimal maxPrice,
        @Schema(description = "Whether any variant has available stock") boolean inStock,
        @Schema(description = "Primary image URL") String imageUrl,
        @Schema(description = "Relevance score", example = "7.42") Float score
) {
}
