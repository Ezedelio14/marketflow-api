package com.marketflow.api.domain.cart;

import com.marketflow.api.domain.common.AuditableEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent cart. The application layer keeps a write-through snapshot of it in Redis.
 */
@Getter
@Entity
@Table(name = "carts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends AuditableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public static Cart openFor(UUID userId) {
        Cart cart = new Cart();
        cart.userId = userId;
        cart.status = CartStatus.ACTIVE;
        return cart;
    }

    public CartItem addItem(UUID variantId, int quantity, BigDecimal unitPrice) {
        ensureActive();
        Optional<CartItem> existing = findItem(variantId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.changeQuantity(item.getQuantity() + quantity);
            item.refreshPrice(unitPrice);
            return item;
        }
        CartItem item = CartItem.of(this, variantId, quantity, unitPrice);
        items.add(item);
        return item;
    }

    public void updateQuantity(UUID variantId, int quantity) {
        ensureActive();
        CartItem item = findItem(variantId).orElseThrow(() -> new ResourceNotFoundException("CartItem", variantId));
        item.changeQuantity(quantity);
    }

    public void removeItem(UUID variantId) {
        ensureActive();
        items.removeIf(i -> i.getProductVariantId().equals(variantId));
    }

    public void clear() {
        items.clear();
    }

    public void checkout() {
        ensureActive();
        if (items.isEmpty()) {
            throw new BusinessRuleException("EMPTY_CART", "Cannot checkout an empty cart");
        }
        status = CartStatus.CHECKED_OUT;
    }

    /** Compensation when checkout could not be completed (e.g. payment provider unavailable). */
    public void reopen() {
        if (status == CartStatus.CHECKED_OUT) {
            status = CartStatus.ACTIVE;
        }
    }

    public BigDecimal subtotal() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private Optional<CartItem> findItem(UUID variantId) {
        return items.stream().filter(i -> i.getProductVariantId().equals(variantId)).findFirst();
    }

    private void ensureActive() {
        if (status != CartStatus.ACTIVE) {
            throw new BusinessRuleException("CART_NOT_ACTIVE", "Cart is no longer active");
        }
    }
}
