package com.marketflow.api.infrastructure.scheduling;

import com.marketflow.api.application.inventory.InventoryService;
import com.marketflow.api.application.order.OrderCancellationService;
import com.marketflow.api.domain.inventory.InventoryReservationRepository;
import com.marketflow.api.domain.order.OrderRepository;
import com.marketflow.api.domain.order.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Enforces the reservation TTL: orders still awaiting payment when their reservation expires are cancelled and
 * the stock is returned. Each order is handled in its own transaction; the {@code @Version} column on orders makes
 * a race with a late "payment succeeded" webhook safe (one of them fails and retries against the new state).
 *
 * <p>With several API instances, add a distributed lock (e.g. ShedLock) to avoid duplicate scans.</p>
 */
@Slf4j
@Component
public class ReservationExpirationJob {

    private static final int BATCH_SIZE = 100;
    private static final String ACTOR = "reservation-expiry-job";

    private final InventoryReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final OrderCancellationService cancellationService;
    private final InventoryService inventoryService;
    private final Clock clock;
    private final TransactionTemplate tx;

    public ReservationExpirationJob(InventoryReservationRepository reservationRepository, OrderRepository orderRepository,
                                    OrderCancellationService cancellationService, InventoryService inventoryService,
                                    Clock clock, PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.cancellationService = cancellationService;
        this.inventoryService = inventoryService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${marketflow.checkout.expiration-job-delay}", initialDelayString = "PT30S")
    public void expireReservations() {
        List<UUID> orderIds = reservationRepository.findOrderIdsWithExpiredReservations(Instant.now(clock), PageRequest.of(0, BATCH_SIZE));
        for (UUID orderId : orderIds) {
            try {
                tx.executeWithoutResult(status -> orderRepository.findWithItemsById(orderId).ifPresent(order -> {
                    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                        cancellationService.cancel(order, "Payment window expired", ACTOR, true);
                    } else {
                        inventoryService.release(orderId, "Reservation expired");
                    }
                }));
            } catch (RuntimeException e) {
                log.warn("Could not expire reservations of order {}: {}", orderId, e.getMessage());
            }
        }
        if (!orderIds.isEmpty()) {
            log.info("Expired reservations of {} order(s)", orderIds.size());
        }
    }
}
