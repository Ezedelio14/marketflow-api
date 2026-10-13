package com.marketflow.api.domain.shipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findFirstByOrderIdOrderByShippedAtDesc(UUID orderId);
}
