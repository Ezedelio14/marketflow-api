package com.marketflow.api.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);

    List<Category> findByStatusOrderByNameAsc(CategoryStatus status);
}
