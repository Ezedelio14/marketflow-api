package com.marketflow.api.domain.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findFirstByUserIdAndStatus(UUID userId, CartStatus status);

    Optional<Cart> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, CartStatus status);
}
