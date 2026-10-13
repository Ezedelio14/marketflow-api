package com.marketflow.api.application.catalog;

import com.marketflow.api.application.catalog.dto.AddImageRequest;
import com.marketflow.api.application.catalog.dto.CreateProductRequest;
import com.marketflow.api.application.catalog.dto.CreateVariantRequest;
import com.marketflow.api.application.catalog.dto.ProductResponse;
import com.marketflow.api.application.catalog.dto.ProductSummaryResponse;
import com.marketflow.api.application.catalog.dto.UpdateProductRequest;
import com.marketflow.api.application.common.Actor;
import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.common.SlugGenerator;
import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.event.Events.IndexOperation;
import com.marketflow.api.application.seller.SellerService;
import com.marketflow.api.domain.catalog.Brand;
import com.marketflow.api.domain.catalog.BrandRepository;
import com.marketflow.api.domain.catalog.Category;
import com.marketflow.api.domain.catalog.CategoryRepository;
import com.marketflow.api.domain.catalog.Product;
import com.marketflow.api.domain.catalog.ProductFilter;
import com.marketflow.api.domain.catalog.ProductRepository;
import com.marketflow.api.domain.catalog.ProductSpecifications;
import com.marketflow.api.domain.catalog.ProductStatus;
import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.catalog.ProductVariantRepository;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.ForbiddenOperationException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.inventory.Inventory;
import com.marketflow.api.domain.inventory.InventoryRepository;
import com.marketflow.api.domain.seller.Seller;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seller-facing catalog management. Every change that affects the storefront emits {@code product.indexed} so the
 * Elasticsearch index is refreshed asynchronously.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;
    private final SellerService sellerService;
    private final ProductMapper mapper;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;

    @Transactional
    public ProductResponse create(Actor actor, CreateProductRequest request) {
        Seller seller = sellerService.requireSellerOf(actor);
        Category category = findCategory(request.categoryId());
        Brand brand = request.brandId() == null ? null : findBrand(request.brandId());
        String slug = SlugGenerator.unique(request.name(), productRepository::existsBySlug);

        Product product = Product.draft(seller, category, brand, request.name(), slug, request.description());
        List<CreateVariantRequest> variants = request.variants() == null ? List.of() : request.variants();
        ensureUniqueSkus(variants);
        productRepository.save(product);
        variants.forEach(v -> addVariantWithStock(product, v));
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(Actor actor, UUID productId, UpdateProductRequest request) {
        Product product = findOwned(actor, productId);
        product.updateDetails(
                request.categoryId() == null ? null : findCategory(request.categoryId()),
                request.brandId() == null ? null : findBrand(request.brandId()),
                request.name(), request.description());
        reindex(product);
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse addVariant(Actor actor, UUID productId, CreateVariantRequest request) {
        Product product = findOwned(actor, productId);
        ensureUniqueSkus(List.of(request));
        addVariantWithStock(product, request);
        reindex(product);
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse addImage(Actor actor, UUID productId, AddImageRequest request) {
        Product product = findOwned(actor, productId);
        product.addImage(request.url(), request.storageKey(), request.altText());
        reindex(product);
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse publish(Actor actor, UUID productId) {
        Product product = findOwned(actor, productId);
        product.publish();
        reindex(product);
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse archive(Actor actor, UUID productId) {
        Product product = findOwned(actor, productId);
        product.archive();
        reindex(product);
        return mapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Actor actor, UUID productId) {
        return mapper.toResponse(findOwned(actor, productId));
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublishedBySlug(String slug) {
        return productRepository.findBySlugAndStatus(slug, ProductStatus.PUBLISHED)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
    }

    /** Back-office listing using JPA Specifications; sellers only ever see their own products. */
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> list(Actor actor, ProductFilter filter, Pageable pageable) {
        ProductFilter effective = actor.isAdmin() ? filter : filter.withSeller(sellerService.requireSellerOf(actor).getId());
        return PageResponse.from(productRepository.findAll(ProductSpecifications.withFilter(effective), pageable), mapper::toSummary);
    }

    private void addVariantWithStock(Product product, CreateVariantRequest v) {
        ProductVariant variant = product.addVariant(v.sku(), v.name(), v.price(), v.compareAtPrice(), v.weight());
        variantRepository.save(variant);
        inventoryRepository.save(Inventory.forVariant(variant, v.initialStock()));
    }

    private void ensureUniqueSkus(List<CreateVariantRequest> variants) {
        Set<String> seen = new HashSet<>();
        for (CreateVariantRequest v : variants) {
            String sku = v.sku().trim().toUpperCase();
            if (!seen.add(sku) || variantRepository.existsBySku(sku)) {
                throw new ConflictException("SKU_IN_USE", "SKU already in use: " + sku);
            }
        }
    }

    private void reindex(Product product) {
        IndexOperation op = product.isPublished() ? IndexOperation.INDEX : IndexOperation.DELETE;
        eventPublisher.publish(eventFactory.productIndexed(product.getId(), op));
    }

    private Product findOwned(Actor actor, UUID productId) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!actor.isAdmin() && !product.getSeller().getUserId().equals(actor.userId())) {
            throw new ForbiddenOperationException("Product belongs to another seller");
        }
        return product;
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private Brand findBrand(UUID id) {
        return brandRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Brand", id));
    }
}
