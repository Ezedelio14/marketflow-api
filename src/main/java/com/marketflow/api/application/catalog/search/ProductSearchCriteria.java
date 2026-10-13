package com.marketflow.api.application.catalog.search;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Storefront search criteria (bound from query parameters)")
public record ProductSearchCriteria(
        @Schema(description = "Full-text query over name, description, brand, category and variant names", example = "phone 128gb")
        String q,
        @Schema(description = "Category ids (OR)") List<UUID> categoryIds,
        @Schema(description = "Brand ids (OR)") List<UUID> brandIds,
        @Schema(description = "Seller id") UUID sellerId,
        @Schema(description = "Minimum variant price", example = "100.00") BigDecimal minPrice,
        @Schema(description = "Maximum variant price", example = "3000.00") BigDecimal maxPrice,
        @Schema(description = "Variant attribute/name filter (e.g. colour or size)", example = "black") String variant,
        @Schema(description = "Only products with available stock", example = "true") Boolean inStock,
        @Schema(description = "Sort order", example = "RELEVANCE") ProductSearchSort sort,
        @Schema(description = "Zero-based page index", example = "0") Integer page,
        @Schema(description = "Page size (max 100)", example = "20") Integer size
) {
    public int pageOrDefault() {
        return page == null || page < 0 ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null || size <= 0 ? 20 : Math.min(size, 100);
    }

    public ProductSearchSort sortOrDefault() {
        return sort == null ? ProductSearchSort.RELEVANCE : sort;
    }
}
