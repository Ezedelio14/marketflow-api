package com.marketflow.api.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /**
     * Atomic, race-free usage increment: returns 0 when the limit was reached concurrently.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
            update Coupon c set c.usedCount = c.usedCount + 1
            where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)
            """)
    int incrementUsage(UUID id);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where upper(c.code) = upper(:code) and c.usedCount > 0")
    int decrementUsage(String code);
}
