package com.marketflow.api.domain.cart;

import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    public static final int MAX_QUANTITY_PER_ITEM = 99;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static CartItem of(Cart cart, UUID productVariantId, int quantity, BigDecimal unitPrice) {
        CartItem item = new CartItem();
        item.cart = cart;
        item.productVariantId = productVariantId;
        item.changeQuantity(quantity);
        item.unitPrice = unitPrice;
        return item;
    }

    void changeQuantity(int newQuantity) {
        if (newQuantity <= 0 || newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity must be between 1 and " + MAX_QUANTITY_PER_ITEM);
        }
        this.quantity = newQuantity;
    }

    void refreshPrice(BigDecimal currentPrice) {
        this.unitPrice = currentPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
