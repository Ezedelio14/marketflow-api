package com.marketflow.api.domain.wishlist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Wishlist> findByUserId(UUID userId);
}
