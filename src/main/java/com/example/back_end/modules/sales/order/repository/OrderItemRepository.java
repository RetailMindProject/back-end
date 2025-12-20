package com.example.back_end.modules.sales.order.repository;

import com.example.back_end.modules.sales.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items for an order
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId ORDER BY oi.id")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Find item by order and product (to check if product already in cart)
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.product.id = :productId")
    Optional<OrderItem> findByOrderIdAndProductId(@Param("orderId") Long orderId, @Param("productId") Long productId);

    /**
     * Delete all items for an order
     */
    void deleteByOrderId(Long orderId);
}