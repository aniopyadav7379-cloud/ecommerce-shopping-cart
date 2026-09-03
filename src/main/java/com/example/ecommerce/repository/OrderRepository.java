package com.example.ecommerce.repository;

import com.example.ecommerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Fetch-joins order items (and their products) so an order can be
     * rendered/serialized after the transaction has closed — the app runs
     * with spring.jpa.open-in-view=false, so views must never depend on
     * lazy loading after the service method returns.
     */
    @Query("select distinct o from Order o left join fetch o.items i left join fetch i.product where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
