package com.example.back_end.modules.sales.returns.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only queries for returns history.
 */
public interface ReturnHistoryRepository extends Repository<com.example.back_end.modules.sales.order.entity.Order, Long> {

    /**
     * Native projection for returned orders summary.
     * Using interface projection avoids DTO constructor/type mismatch issues with native queries.
     */
    interface ReturnedOrderSummaryRow {
        Long getOrderId();
        String getOrderNumber();
        LocalDateTime getOrderDate();
        String getCustomerName();
        BigDecimal getTotalPaid();
        Long getReturnCount();
        BigDecimal getTotalReturned();
        LocalDateTime getLastReturnAt();
    }

    @Query(value = """
            SELECT
              o.id           AS orderId,
              o.order_number AS orderNumber,
              o.created_at   AS orderDate,
              CASE
                WHEN c.id IS NULL THEN NULL
                ELSE TRIM(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, '')))
              END AS customerName,
              o.grand_total  AS totalPaid,
              COUNT(ro.id)   AS returnCount,
              COALESCE(SUM(ro.grand_total), 0) AS totalReturned,
              MAX(ro.created_at) AS lastReturnAt
            FROM orders o
            JOIN orders ro
              ON ro.parent_order_id = o.id
             AND ro.status = 'RETURNED'
            LEFT JOIN customers c
              ON c.id = o.customer_id
            WHERE o.created_at >= COALESCE(CAST(:from AS timestamp), '-infinity'::timestamp)
              AND o.created_at <= COALESCE(CAST(:to   AS timestamp),  'infinity'::timestamp)
              AND (
                    NULLIF(COALESCE(CAST(:q AS text), ''), '') IS NULL
                    OR LOWER(o.order_number) LIKE CONCAT('%', LOWER(CAST(:q AS text)), '%')
                  )
            GROUP BY o.id, o.order_number, o.created_at, o.grand_total, c.id, c.first_name, c.last_name
            ORDER BY MAX(ro.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM (
              SELECT o.id
              FROM orders o
              JOIN orders ro
                ON ro.parent_order_id = o.id
               AND ro.status = 'RETURNED'
              LEFT JOIN customers c
                ON c.id = o.customer_id
              WHERE o.created_at >= COALESCE(CAST(:from AS timestamp), '-infinity'::timestamp)
                AND o.created_at <= COALESCE(CAST(:to   AS timestamp),  'infinity'::timestamp)
                AND (
                      NULLIF(COALESCE(CAST(:q AS text), ''), '') IS NULL
                      OR LOWER(o.order_number) LIKE CONCAT('%', LOWER(CAST(:q AS text)), '%')
                    )
              GROUP BY o.id
            ) t
            """,
            nativeQuery = true)
    Page<ReturnedOrderSummaryRow> findReturnedOrders(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("q") String q,
            Pageable pageable);
}
