package com.marketflow.api.domain.shipment;

import com.marketflow.api.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "shipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "carrier", nullable = false, length = 100)
    private String carrier;

    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    public static Shipment dispatch(UUID orderId, String carrier, String trackingNumber) {
        Shipment shipment = new Shipment();
        shipment.orderId = orderId;
        shipment.carrier = carrier;
        shipment.trackingNumber = trackingNumber;
        shipment.status = ShipmentStatus.SHIPPED;
        shipment.shippedAt = Instant.now();
        return shipment;
    }

    public void markDelivered() {
        status = ShipmentStatus.DELIVERED;
        deliveredAt = Instant.now();
    }
}
