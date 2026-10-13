package com.marketflow.api.domain.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"seller", "category", "brand", "variants"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"seller", "category", "brand", "variants"})
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
}
