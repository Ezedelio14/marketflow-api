package com.marketflow.api.infrastructure.search;

import com.marketflow.api.application.catalog.search.ProductIndexer;
import com.marketflow.api.domain.catalog.Product;
import com.marketflow.api.domain.catalog.ProductImage;
import com.marketflow.api.domain.catalog.ProductRepository;
import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.inventory.Inventory;
import com.marketflow.api.domain.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter pattern: projects the JPA {@link Product} aggregate (plus stock) onto the Elasticsearch
 * {@link ProductDocument}. Invoked asynchronously from Kafka consumers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchSyncAdapter implements ProductIndexer {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductSearchRepository searchRepository;

    @Override
    @Transactional(readOnly = true)
    public void sync(UUID productId) {
        Optional<Product> maybeProduct = productRepository.findDetailedById(productId);
        if (maybeProduct.isEmpty() || !maybeProduct.get().isPublished()) {
            remove(productId);
            return;
        }
        Product product = maybeProduct.get();
        List<ProductVariant> variants = product.getVariants().stream().filter(ProductVariant::isActive).toList();
        Map<UUID, Integer> available = inventoryRepository.findAllByVariantIds(variants.stream().map(ProductVariant::getId).toList())
                .stream().collect(Collectors.toMap(i -> i.getVariant().getId(), Inventory::getAvailableQuantity));

        List<ProductDocument.VariantDocument> variantDocs = variants.stream()
                .map(v -> ProductDocument.VariantDocument.builder()
                        .id(v.getId().toString())
                        .sku(v.getSku())
                        .name(v.getName())
                        .price(v.getPrice().doubleValue())
                        .available(available.getOrDefault(v.getId(), 0))
                        .build())
                .toList();

        ProductDocument document = ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .status(product.getStatus().name())
                .categoryId(product.getCategory().getId().toString())
                .categoryName(product.getCategory().getName())
                .brandId(product.getBrand() == null ? null : product.getBrand().getId().toString())
                .brandName(product.getBrand() == null ? null : product.getBrand().getName())
                .sellerId(product.getSeller().getId().toString())
                .sellerName(product.getSeller().getBusinessName())
                .minPrice(product.minPrice().map(BigDecimal::doubleValue).orElse(null))
                .maxPrice(product.maxPrice().map(BigDecimal::doubleValue).orElse(null))
                .inStock(variantDocs.stream().anyMatch(v -> v.getAvailable() > 0))
                .variantText(variants.stream().map(v -> v.getName() + " " + v.getSku()).collect(Collectors.joining(" ")))
                .variants(variantDocs)
                .imageUrl(product.getImages().stream().findFirst().map(ProductImage::getUrl).orElse(null))
                .createdAtEpochMs(product.getCreatedAt() == null ? null : product.getCreatedAt().toEpochMilli())
                .build();
        searchRepository.save(document);
        log.debug("Indexed product {}", productId);
    }

    @Override
    public void remove(UUID productId) {
        searchRepository.deleteById(productId.toString());
    }
}
