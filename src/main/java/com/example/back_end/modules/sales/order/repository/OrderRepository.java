package com.example.back_end.modules.sales.order.repository;

import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.dashboard.storedashboard.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find all orders by session (for order history)
     */
    @Query("SELECT o FROM Order o WHERE o.session.id = :sessionId ORDER BY o.createdAt DESC")
    List<Order> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find orders by status and session
     */
    @Query("SELECT o FROM Order o WHERE o.session.id = :sessionId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") Order.OrderStatus status);

    /**
     * Find draft/held orders by session (for retrieving saved orders)
     */
    @Query("SELECT o FROM Order o WHERE o.session.id = :sessionId AND o.status IN ('DRAFT', 'HELD') ORDER BY o.createdAt DESC")
    List<Order> findDraftOrdersBySession(@Param("sessionId") Long sessionId);

    /**
     * Count orders in session
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.session.id = :sessionId AND o.status = 'PAID'")
    Long countPaidOrdersBySession(@Param("sessionId") Long sessionId);

    /**
     * Sum total sales in session
     */
    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.session.id = :sessionId AND o.status = 'PAID'")
    BigDecimal sumSalesBySession(@Param("sessionId") Long sessionId);

    // Total sales amount (grand_total) for PAID orders in period
    @Query(value = """
        SELECT COALESCE(SUM(grand_total), 0)
        FROM orders
        WHERE status = 'PAID'
          AND paid_at >= :from
        """, nativeQuery = true)
    BigDecimal sumSalesSince(@Param("from") LocalDateTime from);

    // Count PAID orders in period
    @Query(value = """
        SELECT COUNT(*)
        FROM orders
        WHERE status = 'PAID'
          AND paid_at >= :from
        """, nativeQuery = true)
    Long countOrdersSince(@Param("from") LocalDateTime from);

    // Daily sales aggregation (date, total amount, count)
    @Query(value = """
        SELECT 
            paid_at::date AS saleDate,
            SUM(grand_total) AS totalAmount,
            COUNT(*) AS orderCount
        FROM orders
        WHERE status = 'PAID'
          AND paid_at >= :from
        GROUP BY paid_at::date
        ORDER BY paid_at::date
        """, nativeQuery = true)
    List<StoreDailySalesProjection> findDailySalesSince(@Param("from") LocalDateTime from);

    // Weekly sales trend with revenue (line chart data)
    @Query(value = """
        SELECT 
            paid_at::date AS saleDate,
            SUM(grand_total) AS revenue,
            COUNT(*) AS orders
        FROM orders
        WHERE status = 'PAID'
          AND paid_at >= :from
        GROUP BY paid_at::date
        ORDER BY paid_at::date
        """, nativeQuery = true)
    List<StoreWeeklySalesProjection> findWeeklySalesTrend(@Param("from") LocalDateTime from);

    // Category product counts (pie chart: how many products sold per category)
    @Query(value = """
        SELECT 
            c.name AS categoryName,
            COUNT(DISTINCT oi.product_id) AS productCount
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN products p ON p.id = oi.product_id
        JOIN product_categories pc ON pc.product_id = p.id
        JOIN categories c ON c.id = pc.category_id
        WHERE o.status = 'PAID'
          AND o.paid_at >= :from
        GROUP BY c.name
        ORDER BY productCount DESC
        """, nativeQuery = true)
    List<StoreCategoryCountProjection> findCategoryProductCounts(@Param("from") LocalDateTime from);

    // Top products by quantity sold
    @Query(value = """
        SELECT 
            p.id AS productId,
            p.name AS productName,
            p.sku AS sku,
            SUM(oi.quantity) AS sold,
            SUM(oi.line_total) AS revenue
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN products p ON p.id = oi.product_id
        WHERE o.status = 'PAID'
          AND o.paid_at >= :from
        GROUP BY p.id, p.name, p.sku
        ORDER BY SUM(oi.quantity) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<StoreTopProductProjection> findTopProducts(@Param("from") LocalDateTime from, @Param("limit") int limit);

    // Most popular product (single result)
    @Query(value = """
        SELECT 
            p.id AS productId,
            p.name AS productName,
            SUM(oi.quantity) AS sold,
            SUM(oi.line_total) AS revenue
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN products p ON p.id = oi.product_id
        WHERE o.status = 'PAID'
          AND o.paid_at >= :from
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
        LIMIT 1
        """, nativeQuery = true)
    StoreTopProductProjection findMostPopularProduct(@Param("from") LocalDateTime from);
}

