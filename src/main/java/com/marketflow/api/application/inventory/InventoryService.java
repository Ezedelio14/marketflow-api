package com.marketflow.api.application.inventory;

import com.marketflow.api.application.common.Actor;
import com.marketflow.api.application.event.DomainEventFactory;
import com.marketflow.api.application.event.EventPublisher;
import com.marketflow.api.application.event.Events.IndexOperation;
import com.marketflow.api.application.inventory.dto.InventoryResponse;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.ForbiddenOperationException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.inventory.Inventory;
import com.marketflow.api.domain.inventory.InventoryRepository;
import com.marketflow.api.domain.inventory.InventoryReservation;
import com.marketflow.api.domain.inventory.InventoryReservationRepository;
import com.marketflow.api.domain.inventory.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stock reservation lifecycle: reserve (checkout) → confirm (payment succeeded) or release (cancel/expiry).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int ADJUST_MAX_ATTEMPTS = 3;

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final PlatformTransactionManager transactionManager;
    private final EventPublisher eventPublisher;
    private final DomainEventFactory eventFactory;

    /**
     * Reserves every line of an order inside the caller's transaction. With {@link LockStrategy#PESSIMISTIC} the
     * rows are locked ({@code FOR UPDATE}) in a deterministic order; with {@link LockStrategy#OPTIMISTIC} a
     * concurrent writer makes the flush fail with {@link ObjectOptimisticLockingFailureException}, which the
     * checkout facade turns into a retry (and eventually a pessimistic fallback).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<InventoryReservation> reserve(UUID orderId, Map<UUID, Integer> quantitiesByVariant,
                                              Instant expiresAt, LockStrategy strategy) {
        List<Inventory> inventories = strategy == LockStrategy.PESSIMISTIC
                ? inventoryRepository.findAllByVariantIdsForUpdate(quantitiesByVariant.keySet())
                : inventoryRepository.findAllByVariantIds(quantitiesByVariant.keySet());

        Map<UUID, Inventory> byVariant = inventories.stream()
                .collect(Collectors.toMap(i -> i.getVariant().getId(), Function.identity()));

        List<InventoryReservation> reservations = new ArrayList<>();
        quantitiesByVariant.forEach((variantId, quantity) -> {
            Inventory inventory = byVariant.get(variantId);
            if (inventory == null) {
                throw new ResourceNotFoundException("Inventory for variant", variantId);
            }
            reservations.add(InventoryReservation.hold(inventory, orderId, quantity, expiresAt));
        });
        reservationRepository.saveAll(reservations);
        inventoryRepository.flush(); // surface @Version conflicts now, inside the checkout attempt
        return reservations;
    }

    /** Payment captured: reserved units are deducted from on-hand stock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<InventoryReservation> confirm(UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.PENDING);
        reservations.forEach(InventoryReservation::confirm);
        return reservations;
    }

    /** Order cancelled or reservation expired: units return to the available pool. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<InventoryReservation> release(UUID orderId, String reason) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.PENDING);
        if (reservations.isEmpty()) {
            return reservations;
        }
        reservations.forEach(InventoryReservation::release);
        eventPublisher.publish(eventFactory.inventoryReleased(orderId, reservations, reason));
        log.info("Released {} reservation(s) of order {} ({})", reservations.size(), orderId, reason);
        return reservations;
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Actor actor, UUID variantId) {
        Inventory inventory = findByVariant(variantId);
        ensureOwner(actor, inventory);
        return InventoryResponse.from(inventory);
    }

    /**
     * Seller stock adjustment protected by optimistic locking, retried on version conflicts because adjustments
     * race with checkouts on hot products.
     */
    public InventoryResponse adjust(Actor actor, UUID variantId, int newQuantity) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        for (int attempt = 1; ; attempt++) {
            try {
                InventoryResponse response = tx.execute(status -> {
                    Inventory inventory = findByVariant(variantId);
                    ensureOwner(actor, inventory);
                    inventory.adjustQuantity(newQuantity);
                    inventoryRepository.flush();
                    eventPublisher.publish(eventFactory.productIndexed(inventory.getVariant().getProduct().getId(), IndexOperation.INDEX));
                    return InventoryResponse.from(inventory);
                });
                return response;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= ADJUST_MAX_ATTEMPTS) {
                    throw new ConflictException("CONCURRENT_INVENTORY_UPDATE", "Inventory changed concurrently, please retry");
                }
                log.debug("Optimistic lock conflict adjusting variant {} (attempt {})", variantId, attempt);
            }
        }
    }

    private Inventory findByVariant(UUID variantId) {
        return inventoryRepository.findByVariant_Id(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for variant", variantId));
    }

    private static void ensureOwner(Actor actor, Inventory inventory) {
        if (!actor.isAdmin() && !inventory.getVariant().getProduct().getSeller().getUserId().equals(actor.userId())) {
            throw new ForbiddenOperationException("Variant belongs to another seller");
        }
    }
}
