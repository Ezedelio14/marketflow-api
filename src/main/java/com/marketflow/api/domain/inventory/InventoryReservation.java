package com.marketflow.api.domain.inventory;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock held for an order while it awaits payment. Expires at {@code expiresAt} (TTL), after which the
 * expiration job releases it back to {@link Inventory}.
 */
@Getter
@Entity
@Table(
        name = "inventory_reservations",
        indexes = {
                @Index(name = "idx_reservations_order", columnList = "order_id"),
                @Index(name = "idx_reservations_status_expires", columnList = "status, expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static InventoryReservation hold(Inventory inventory, UUID orderId, int quantity, Instant expiresAt) {
        inventory.reserve(quantity);
        InventoryReservation reservation = new InventoryReservation();
        reservation.inventory = inventory;
        reservation.orderId = orderId;
        reservation.quantity = quantity;
        reservation.status = ReservationStatus.PENDING;
        reservation.expiresAt = expiresAt;
        return reservation;
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) return;
        inventory.commitReservation(quantity);
        status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        if (status != ReservationStatus.PENDING) return;
        inventory.releaseReservation(quantity);
        status = ReservationStatus.RELEASED;
    }

    public boolean isExpired(Instant now) {
        return status == ReservationStatus.PENDING && expiresAt.isBefore(now);
    }
}
