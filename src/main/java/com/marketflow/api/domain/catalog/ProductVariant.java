package com.marketflow.api.domain.catalog;

import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Sellable unit of a product (size/colour/etc.). Price and SKU live here; stock lives in {@code Inventory}.
 */
@Getter
@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_variants_sku", columnNames = "sku"),
        indexes = @Index(name = "idx_product_variants_product", columnList = "product_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 19, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VariantStatus status;

    static ProductVariant create(Product product, String sku, String name, BigDecimal price,
                                 BigDecimal compareAtPrice, BigDecimal weight) {
        ProductVariant variant = new ProductVariant();
        variant.product = product;
        variant.sku = sku.trim().toUpperCase();
        variant.name = name;
        variant.changePrice(price, compareAtPrice);
        variant.weight = weight == null ? BigDecimal.ZERO : weight;
        variant.status = VariantStatus.ACTIVE;
        return variant;
    }

    public void changePrice(BigDecimal newPrice, BigDecimal newCompareAtPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new BusinessRuleException("INVALID_PRICE", "Price must be greater than zero");
        }
        if (newCompareAtPrice != null && newCompareAtPrice.compareTo(newPrice) < 0) {
            throw new BusinessRuleException("INVALID_COMPARE_AT_PRICE", "Compare-at price must be greater than or equal to price");
        }
        this.price = newPrice;
        this.compareAtPrice = newCompareAtPrice;
    }

    public void deactivate() {
        status = VariantStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == VariantStatus.ACTIVE;
    }

    public boolean isPurchasable() {
        return isActive() && product.isPublished();
    }
}
