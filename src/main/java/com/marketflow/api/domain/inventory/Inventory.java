package com.marketflow.api.domain.inventory;

import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Stock of a single product variant.
 *
 * <p>Invariant: {@code availableQuantity = quantity - reservedQuantity} and never negative.
 * Concurrent writers are serialised by the {@link Version} column (optimistic locking); the checkout
 * path can additionally take a {@code PESSIMISTIC_WRITE} row lock (see {@code InventoryRepository}).</p>
 */
@Getter
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventories_variant", columnNames = "product_variant_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false, updatable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Inventory forVariant(ProductVariant variant, int initialQuantity) {
        if (initialQuantity < 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity cannot be negative");
        }
        Inventory inventory = new Inventory();
        inventory.variant = variant;
        inventory.quantity = initialQuantity;
        inventory.reservedQuantity = 0;
        inventory.recalculate();
        return inventory;
    }

    /** Holds stock for a pending order. */
    public void reserve(int amount) {
        requirePositive(amount);
        if (amount > availableQuantity) {
            throw new InsufficientStockException(variant.getId(), amount, availableQuantity);
        }
        reservedQuantity += amount;
        recalculate();
    }

    /** Payment confirmed: reserved units leave the warehouse for good. */
    public void commitReservation(int amount) {
        requirePositive(amount);
        if (amount > reservedQuantity) {
            throw new BusinessRuleException("INVALID_RESERVATION", "Cannot commit more than reserved");
        }
        reservedQuantity -= amount;
        quantity -= amount;
        recalculate();
    }

    /** Reservation cancelled or expired: units become available again. */
    public void releaseReservation(int amount) {
        requirePositive(amount);
        reservedQuantity = Math.max(0, reservedQuantity - amount);
        recalculate();
    }

    /** Seller sets the physical on-hand quantity. */
    public void adjustQuantity(int newQuantity) {
        if (newQuantity < reservedQuantity) {
            throw new BusinessRuleException("INVALID_QUANTITY",
                    "Quantity (%d) cannot be lower than reserved units (%d)".formatted(newQuantity, reservedQuantity));
        }
        quantity = newQuantity;
        recalculate();
    }

    public boolean isInStock() {
        return availableQuantity > 0;
    }

    private void recalculate() {
        availableQuantity = quantity - reservedQuantity;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity must be positive");
        }
    }
}
