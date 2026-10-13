package com.marketflow.api.domain.order;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Specification pattern for order queries (customer history, seller fulfilment queue, admin back-office).
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> withFilter(OrderFilter filter) {
        return Specification.allOf(
                hasStatusIn(filter.statuses()),
                placedBy(filter.userId()),
                containsItemsFromSeller(filter.sellerId()),
                orderNumberStartsWith(filter.orderNumber()),
                createdBetween(filter.createdFrom(), filter.createdTo()),
                totalBetween(filter.minTotal(), filter.maxTotal())
        );
    }

    public static Specification<Order> hasStatusIn(Collection<OrderStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Order> placedBy(UUID userId) {
        return userId == null ? null : (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    /** Multi-vendor: orders with at least one line sold by the seller. */
    public static Specification<Order> containsItemsFromSeller(UUID sellerId) {
        if (sellerId == null) return null;
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<OrderItem> item = sub.from(OrderItem.class);
            sub.select(item.get("id")).where(cb.equal(item.get("order"), root), cb.equal(item.get("sellerId"), sellerId));
            return cb.exists(sub);
        };
    }

    public static Specification<Order> orderNumberStartsWith(String prefix) {
        if (prefix == null || prefix.isBlank()) return null;
        return (root, query, cb) -> cb.like(root.get("orderNumber"), prefix.trim().toUpperCase() + "%");
    }

    public static Specification<Order> createdBetween(Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, query, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            return from != null ? cb.greaterThanOrEqualTo(root.get("createdAt"), from) : cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Order> totalBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null) return cb.between(root.get("total"), min, max);
            return min != null ? cb.greaterThanOrEqualTo(root.get("total"), min) : cb.lessThanOrEqualTo(root.get("total"), max);
        };
    }
}
