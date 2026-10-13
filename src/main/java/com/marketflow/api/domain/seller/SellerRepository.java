package com.marketflow.api.domain.seller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByTaxNumber(String taxNumber);
}
