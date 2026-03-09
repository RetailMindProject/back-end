package com.example.back_end.modules.sales.order.repository;

import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.projection.OrderReportProjection;
import com.example.back_end.modules.dashboard.storedashboard.projection.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @Query("SELECT o FROM Order o WHERE o.session.id = :sessionId AND o.status IN ('DRAFT', 'HOLD') ORDER BY o.createdAt DESC")
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

    /**
     * Return orders for a given original order (orders.parent_order_id = :orderId) where status='RETURNED'.
     */
    @Query("SELECT o FROM Order o WHERE o.parentOrderId = :orderId AND o.status = com.example.back_end.modules.sales.order.entity.Order.OrderStatus.RETURNED ORDER BY o.createdAt DESC")
    List<Order> findReturnOrdersByOriginalOrderId(@Param("orderId") Long orderId);

    /**
     * Find orders for a customer (by customer.id from customers table).
     * Used for customer order history.
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") Integer customerId, Pageable pageable);

    /**
     * Find orders for a customer created after a specific date.
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findByCustomerIdAndCreatedAtAfter(@Param("customerId") Integer customerId, @Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Count total orders for a customer.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId")
    Long countByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Count orders for a customer created after a specific date.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId AND o.createdAt >= :since")
    Long countByCustomerIdAndCreatedAtAfter(@Param("customerId") Integer customerId, @Param("since") LocalDateTime since);

    /**
/**
 * Get order report with pagination, filtering, and sorting
 * Joins orders with sessions, users (cashier), customers, and payments
 */
@Query(value = """
    SELECT 
        o.id AS orderId,
        o.order_number AS orderNumber,
        o.created_at AS createdAt,
        o.paid_at AS paidAt,
        o.session_id AS sessionId,
        u.id AS cashierId,
        u.first_name AS cashierFirstName,
        u.last_name AS cashierLastName,
        c.id AS customerId,
        c.first_name AS customerFirstName,
        c.last_name AS customerLastName,
        c.phone AS customerPhone,
        o.status AS status,
        COUNT(DISTINCT oi.id) AS itemCount,
        o.subtotal AS subtotal,
        o.discount_total AS discountAmount,
        o.tax_total AS taxAmount,
        o.grand_total AS grandTotal,
        CASE 
            WHEN COUNT(DISTINCT p.id) > 1 THEN 'SPLIT'
            WHEN COUNT(DISTINCT p.id) = 1 THEN MAX(p.method)
            ELSE 'UNKNOWN'
        END AS paymentMethod,
        COUNT(DISTINCT p.id) AS paymentCount
    FROM orders o
    INNER JOIN sessions s ON s.id = o.session_id
    INNER JOIN users u ON u.id = s.user_id
    LEFT JOIN customers c ON c.id = o.customer_id
    LEFT JOIN payments p ON p.order_id = o.id AND p.type = 'PAYMENT'
    LEFT JOIN order_items oi ON oi.order_id = o.id
    WHERE o.status = COALESCE(:status, 'PAID')
      AND (
        (:fromDate IS NULL AND :toDate IS NULL)
        OR (COALESCE(o.paid_at, o.created_at)::date >= COALESCE(:fromDate, '1900-01-01'::date))
        AND (COALESCE(o.paid_at, o.created_at)::date <= COALESCE(:toDate, '9999-12-31'::date))
      )
      AND (:cashierName IS NULL OR CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) ILIKE CONCAT('%', :cashierName, '%'))
      AND (:cashierId IS NULL OR u.id = :cashierId)
    GROUP BY o.id, o.order_number, o.created_at, o.paid_at, o.session_id, 
             u.id, u.first_name, u.last_name, c.id, c.first_name, c.last_name, 
             c.phone, o.status, o.subtotal, o.discount_total, o.tax_total, o.grand_total
    ORDER BY 
        CASE WHEN :orderBy = 'date' AND :orderDirection = 'ASC' THEN COALESCE(o.paid_at, o.created_at) END ASC,
        CASE WHEN :orderBy = 'date' AND :orderDirection = 'DESC' THEN COALESCE(o.paid_at, o.created_at) END DESC,
        CASE WHEN :orderBy = 'total' AND :orderDirection = 'ASC' THEN o.grand_total END ASC,
        CASE WHEN :orderBy = 'total' AND :orderDirection = 'DESC' THEN o.grand_total END DESC,
        CASE WHEN :orderBy = 'cashier' AND :orderDirection = 'ASC' THEN CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) END ASC,
        CASE WHEN :orderBy = 'cashier' AND :orderDirection = 'DESC' THEN CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) END DESC,
        COALESCE(o.paid_at, o.created_at) DESC
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
List<OrderReportProjection> findOrderReport(
        @Param("status") String status,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("cashierName") String cashierName,
        @Param("cashierId") Long cashierId,
        @Param("orderBy") String orderBy,
        @Param("orderDirection") String orderDirection,
        @Param("limit") Integer limit,
        @Param("offset") Integer offset
);

/**
 * Count total orders matching the report filters (for pagination)
 */
@Query(value = """
    SELECT COUNT(DISTINCT o.id)
    FROM orders o
    INNER JOIN sessions s ON s.id = o.session_id
    INNER JOIN users u ON u.id = s.user_id
    WHERE o.status = COALESCE(:status, 'PAID')
      AND (
        (:fromDate IS NULL AND :toDate IS NULL)
        OR (COALESCE(o.paid_at, o.created_at)::date >= COALESCE(:fromDate, '1900-01-01'::date))
        AND (COALESCE(o.paid_at, o.created_at)::date <= COALESCE(:toDate, '9999-12-31'::date))
      )
      AND (:cashierName IS NULL OR CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) ILIKE CONCAT('%', :cashierName, '%'))
      AND (:cashierId IS NULL OR u.id = :cashierId)
    """, nativeQuery = true)
Long countOrderReport(
        @Param("status") String status,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("cashierName") String cashierName,
        @Param("cashierId") Long cashierId
);

/**
 * Sum total discount amount for PAID orders in period
 */
@Query(value = """
    SELECT COALESCE(SUM(discount_total), 0)
    FROM orders
    WHERE status = 'PAID'
      AND paid_at >= :from
      AND paid_at <= :to
    """, nativeQuery = true)
BigDecimal sumDiscountSince(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

/**
 * Sum total tax amount for PAID orders in period
 */
@Query(value = """
    SELECT COALESCE(SUM(tax_total), 0)
    FROM orders
    WHERE status = 'PAID'
      AND paid_at >= :from
      AND paid_at <= :to
    """, nativeQuery = true)
BigDecimal sumTaxSince(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

/**
 * Get earliest order date
 */
@Query(value = """
    SELECT MIN(COALESCE(paid_at, created_at))::date
    FROM orders
    WHERE status = 'PAID'
    """, nativeQuery = true)
LocalDate getEarliestOrderDate();

/**
 * Get latest order date
 */
@Query(value = """
    SELECT MAX(COALESCE(paid_at, created_at))::date
    FROM orders
    WHERE status = 'PAID'
    """, nativeQuery = true)
LocalDate getLatestOrderDate();

/**
 * Minimal receipt header fetch.
 * We only need Order header fields (status, totals, order_number, created_at, paid_at).
 */
@Query("SELECT o FROM Order o WHERE o.id = :orderId")
Optional<Order> findReceiptOrderById(@Param("orderId") Long orderId);
}
