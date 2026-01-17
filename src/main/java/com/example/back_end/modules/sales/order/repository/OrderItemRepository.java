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
     * Find all items for an order.
     *
     * Fetch product + its media to allow returning product.image in API responses.
     */
    @Query("""
            SELECT DISTINCT oi
            FROM OrderItem oi
            LEFT JOIN FETCH oi.product p
            LEFT JOIN FETCH p.productMedia pm
            LEFT JOIN FETCH pm.media m
            WHERE oi.order.id = :orderId
            ORDER BY oi.id
            """)
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

    /**
     * Receipt view: order items joined with products.
     */
    @Query(value = """
            SELECT
                oi.id as orderItemId,
                p.sku as sku,
                p.name as name,
                p.unit as unit,
                oi.quantity as quantity,
                oi.unit_price as unitPrice,
                oi.line_total as lineTotal
            FROM order_items oi
            JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = :orderId
            ORDER BY oi.id
            """, nativeQuery = true)
    List<com.example.back_end.modules.sales.receipt.repository.ReceiptItemRow> findReceiptItemsByOrderId(@Param("orderId") Long orderId);
}