package com.marketflow.api.domain.inventory;

import com.marketflow.api.domain.catalog.ProductVariant;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import com.marketflow.api.domain.common.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryTest {

    private final ProductVariant variant = Mockito.mock(ProductVariant.class);

    @Test
    void reserveConfirmAndReleaseKeepTheBalanceInvariant() {
        Inventory inventory = Inventory.forVariant(variant, 10);

        InventoryReservation r1 = InventoryReservation.hold(inventory, UUID.randomUUID(), 3, Instant.now().plusSeconds(60));
        InventoryReservation r2 = InventoryReservation.hold(inventory, UUID.randomUUID(), 4, Instant.now().plusSeconds(60));
        assertThat(inventory.getReservedQuantity()).isEqualTo(7);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(3);

        r1.confirm();
        assertThat(inventory.getQuantity()).isEqualTo(7);
        assertThat(inventory.getReservedQuantity()).isEqualTo(4);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(3);

        r2.release();
        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
        assertThat(r2.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void preventsOverselling() {
        Mockito.when(variant.getId()).thenReturn(UUID.randomUUID());
        Inventory inventory = Inventory.forVariant(variant, 2);
        assertThatThrownBy(() -> inventory.reserve(3)).isInstanceOf(InsufficientStockException.class);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void releasingTwiceIsANoOp() {
        Inventory inventory = Inventory.forVariant(variant, 5);
        InventoryReservation reservation = InventoryReservation.hold(inventory, UUID.randomUUID(), 2, Instant.now());
        reservation.release();
        reservation.release();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void cannotAdjustBelowReservedUnits() {
        Inventory inventory = Inventory.forVariant(variant, 5);
        inventory.reserve(4);
        assertThatThrownBy(() -> inventory.adjustQuantity(3)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void detectsExpiredReservation() {
        Inventory inventory = Inventory.forVariant(variant, 5);
        InventoryReservation reservation = InventoryReservation.hold(inventory, UUID.randomUUID(), 1, Instant.now().minusSeconds(1));
        assertThat(reservation.isExpired(Instant.now())).isTrue();
    }
}
