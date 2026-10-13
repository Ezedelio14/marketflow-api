package com.marketflow.api.domain.catalog;

import com.marketflow.api.domain.common.AuditableEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.seller.Seller;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    public static Product draft(Seller seller, Category category, Brand brand, String name, String slug, String description) {
        seller.ensureCanSell();
        if (!category.isActive()) {
            throw new BusinessRuleException("CATEGORY_INACTIVE", "Category is not active");
        }
        Product product = new Product();
        product.seller = seller;
        product.category = category;
        product.brand = brand;
        product.name = name;
        product.slug = slug;
        product.description = description;
        product.status = ProductStatus.DRAFT;
        return product;
    }

    public void updateDetails(Category category, Brand brand, String name, String description) {
        ensureNotArchived();
        if (category != null) this.category = category;
        if (brand != null) this.brand = brand;
        if (name != null) this.name = name;
        if (description != null) this.description = description;
    }

    public ProductVariant addVariant(String sku, String name, BigDecimal price, BigDecimal compareAtPrice, BigDecimal weight) {
        ensureNotArchived();
        ProductVariant variant = ProductVariant.create(this, sku, name, price, compareAtPrice, weight);
        variants.add(variant);
        return variant;
    }

    public ProductImage addImage(String url, String storageKey, String altText) {
        ProductImage image = ProductImage.create(this, url, storageKey, images.size(), altText);
        images.add(image);
        return image;
    }

    public void publish() {
        ensureNotArchived();
        if (variants.stream().noneMatch(ProductVariant::isActive)) {
            throw new BusinessRuleException("PRODUCT_WITHOUT_VARIANTS", "A product needs at least one active variant to be published");
        }
        status = ProductStatus.PUBLISHED;
    }

    public void archive() {
        status = ProductStatus.ARCHIVED;
    }

    public boolean isPublished() {
        return status == ProductStatus.PUBLISHED;
    }

    public Optional<BigDecimal> minPrice() {
        return variants.stream().filter(ProductVariant::isActive).map(ProductVariant::getPrice).min(Comparator.naturalOrder());
    }

    public Optional<BigDecimal> maxPrice() {
        return variants.stream().filter(ProductVariant::isActive).map(ProductVariant::getPrice).max(Comparator.naturalOrder());
    }

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    private void ensureNotArchived() {
        if (status == ProductStatus.ARCHIVED) {
            throw new BusinessRuleException("PRODUCT_ARCHIVED", "Archived products cannot be modified");
        }
    }
}
