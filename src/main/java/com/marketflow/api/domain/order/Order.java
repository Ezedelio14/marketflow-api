package com.marketflow.api.domain.order;

import com.marketflow.api.domain.common.AuditableEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order aggregate root.
 *
 * <p>Status is never set directly: every change goes through {@code OrderStateMachine}, which validates the
 * transition (State pattern) and then calls {@link #applyTransition}, recording an {@link OrderStatusHistory}
 * entry. Instances are created through {@link Builder} so monetary invariants are checked in one place.</p>
 */
@Getter
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "idx_orders_user", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AuditableEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "order_number", nullable = false, updatable = false, length = 32)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private ShippingAddress shippingAddress;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OrderBy("changedAt ASC")
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Applies a transition already validated by the state machine. Must not be called from anywhere else.
     */
    public void applyTransition(OrderStatus target, String reason, String changedBy) {
        OrderStatus from = this.status;
        this.status = target;
        statusHistory.add(OrderStatusHistory.record(this, from, target, reason, changedBy));
    }

    public boolean belongsTo(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public boolean containsItemsOf(UUID sellerId) {
        return items.stream().anyMatch(i -> i.getSellerId().equals(sellerId));
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    /** Builder pattern: assembles a consistent order and computes the grand total. */
    public static final class Builder {
        private UUID userId;
        private String orderNumber;
        private String currency;
        private String couponCode;
        private ShippingAddress shippingAddress;
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal shippingCost = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private final List<OrderItem> items = new ArrayList<>();

        private Builder() {
        }

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder couponCode(String couponCode) { this.couponCode = couponCode; return this; }
        public Builder shippingAddress(ShippingAddress address) { this.shippingAddress = address; return this; }
        public Builder discount(BigDecimal discount) { this.discount = discount; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder tax(BigDecimal tax) { this.tax = tax; return this; }
        public Builder item(OrderItem item) { this.items.add(item); return this; }
        public Builder items(List<OrderItem> items) { this.items.addAll(items); return this; }

        public Order build() {
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(orderNumber, "orderNumber");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(shippingAddress, "shippingAddress");
            if (items.isEmpty()) {
                throw new BusinessRuleException("ORDER_WITHOUT_ITEMS", "An order must contain at least one item");
            }
            Order order = new Order();
            order.userId = userId;
            order.orderNumber = orderNumber;
            order.currency = currency.toUpperCase();
            order.couponCode = couponCode;
            order.shippingAddress = shippingAddress;
            order.subtotal = scale(items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
            order.discount = scale(discount.min(order.subtotal));
            order.shippingCost = scale(shippingCost);
            order.tax = scale(tax);
            order.total = scale(order.subtotal.subtract(order.discount).add(order.shippingCost).add(order.tax));
            items.forEach(item -> {
                item.attachTo(order);
                order.items.add(item);
            });
            order.applyTransition(OrderStatus.PENDING_PAYMENT, "Order placed", userId.toString());
            return order;
        }

        private static BigDecimal scale(BigDecimal value) {
            return value.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
