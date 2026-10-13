package com.marketflow.api.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
