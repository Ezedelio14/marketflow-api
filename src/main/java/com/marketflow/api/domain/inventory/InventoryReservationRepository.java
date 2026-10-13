package com.marketflow.api.domain.inventory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    @Query("select r from InventoryReservation r join fetch r.inventory i join fetch i.variant where r.orderId = :orderId and r.status = :status")
    List<InventoryReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    @Query("""
            select distinct r.orderId from InventoryReservation r
            where r.status = com.marketflow.api.domain.inventory.ReservationStatus.PENDING
              and r.expiresAt < :now
            """)
    List<UUID> findOrderIdsWithExpiredReservations(Instant now, Pageable pageable);
}
