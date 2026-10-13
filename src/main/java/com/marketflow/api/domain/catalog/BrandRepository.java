package com.marketflow.api.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsBySlug(String slug);
}
