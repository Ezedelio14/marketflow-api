package com.marketflow.api.domain.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    @Query("""
            select v from ProductVariant v
            join fetch v.product p
            join fetch p.seller
            where v.id in :ids
            """)
    List<ProductVariant> findAllWithProductByIdIn(Collection<UUID> ids);
}
