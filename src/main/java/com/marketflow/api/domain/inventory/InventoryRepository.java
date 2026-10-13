package com.marketflow.api.domain.inventory;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByVariant_Id(UUID variantId);

    /** Optimistic read: conflicts are detected on flush via {@code @Version}. */
    @Query("select i from Inventory i join fetch i.variant v where v.id in :variantIds order by i.id")
    List<Inventory> findAllByVariantIds(Collection<UUID> variantIds);

    /**
     * Critical checkout path: {@code SELECT ... FOR UPDATE} on the inventory rows. Rows are always locked in
     * id order so two concurrent checkouts never deadlock on each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select i from Inventory i join fetch i.variant v where v.id in :variantIds order by i.id")
    List<Inventory> findAllByVariantIdsForUpdate(Collection<UUID> variantIds);
}
