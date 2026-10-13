package com.marketflow.api.domain.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilter(
        String query,
        ProductStatus status,
        UUID sellerId,
        UUID categoryId,
        UUID brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sku
) {
    public ProductFilter withSeller(UUID forcedSellerId) {
        return new ProductFilter(query, status, forcedSellerId, categoryId, brandId, minPrice, maxPrice, sku);
    }
}
