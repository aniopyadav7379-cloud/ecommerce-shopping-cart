package com.example.ecommerce.repository;

import com.example.ecommerce.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Fetch-joins the product so cart items can be rendered by Thymeleaf
     * (or serialized to JSON) after the transaction has closed, without
     * triggering a LazyInitializationException — the app runs with
     * spring.jpa.open-in-view=false on purpose, so views must never rely
     * on lazy loading after the service method returns.
     */
    @Query("select ci from CartItem ci join fetch ci.product where ci.cartId = :cartId order by ci.id asc")
    List<CartItem> findByCartIdOrderByIdAsc(@Param("cartId") String cartId);

    @Query("select ci from CartItem ci join fetch ci.product where ci.cartId = :cartId and ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") String cartId, @Param("productId") Long productId);

    void deleteByCartId(String cartId);

    void deleteByCartIdAndProductId(String cartId, Long productId);
}
