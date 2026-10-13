package com.marketflow.api.domain.catalog;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Specification pattern: composable predicates for the back-office product listing. Each method returns
 * {@code null} when its filter is absent, which {@link Specification#allOf} ignores.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilter(ProductFilter filter) {
        return Specification.allOf(
                nameOrDescriptionContains(filter.query()),
                hasStatus(filter.status()),
                belongsToSeller(filter.sellerId()),
                inCategory(filter.categoryId()),
                ofBrand(filter.brandId()),
                hasVariantPricedBetween(filter.minPrice(), filter.maxPrice()),
                hasSku(filter.sku())
        );
    }

    public static Specification<Product> nameOrDescriptionContains(String text) {
        if (text == null || text.isBlank()) return null;
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like));
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> belongsToSeller(UUID sellerId) {
        return sellerId == null ? null : (root, query, cb) -> cb.equal(root.get("seller").get("id"), sellerId);
    }

    public static Specification<Product> inCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> {
            Join<Product, Category> category = root.join("category", JoinType.INNER);
            // Matches the category itself or any direct child category
            return cb.or(cb.equal(category.get("id"), categoryId), cb.equal(category.get("parent").get("id"), categoryId));
        };
    }

    public static Specification<Product> ofBrand(UUID brandId) {
        return brandId == null ? null : (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    /** EXISTS sub-query: at least one active variant within the price range. */
    public static Specification<Product> hasVariantPricedBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<ProductVariant> variant = sub.from(ProductVariant.class);
            var predicate = cb.and(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("status"), VariantStatus.ACTIVE));
            if (min != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(variant.get("price"), min));
            if (max != null) predicate = cb.and(predicate, cb.lessThanOrEqualTo(variant.get("price"), max));
            sub.select(variant.get("id")).where(predicate);
            return cb.exists(sub);
        };
    }

    public static Specification<Product> hasSku(String sku) {
        if (sku == null || sku.isBlank()) return null;
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<ProductVariant> variant = sub.from(ProductVariant.class);
            sub.select(variant.get("id")).where(
                    cb.equal(variant.get("product"), root),
                    cb.equal(variant.get("sku"), sku.trim().toUpperCase()));
            return cb.exists(sub);
        };
    }
}
