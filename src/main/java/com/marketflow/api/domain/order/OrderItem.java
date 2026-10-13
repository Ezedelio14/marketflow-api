package com.marketflow.api.domain.order;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable line of an order. Product name, SKU and price are snapshotted so later catalog changes
 * never alter historical orders. {@code sellerId} enables multi-vendor split/fulfilment.
 */
@Getter
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order", columnList = "order_id"),
                @Index(name = "idx_order_items_seller", columnList = "seller_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    public static OrderItem of(UUID productVariantId, UUID sellerId, String productName, String sku,
                               int quantity, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.productVariantId = productVariantId;
        item.sellerId = sellerId;
        item.productName = productName;
        item.sku = sku;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        item.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    void attachTo(Order order) {
        this.order = order;
    }
}
