package com.marketflow.api.domain.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithItemsById(UUID id);

    boolean existsByOrderNumber(String orderNumber);
}
