package com.example.back_end.modules.cashier.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.back_end.modules.sales.order.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CashierOrderRepository extends JpaRepository<Order, Long> {

    /**
     * Count paid orders for a session
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.session.id = :sessionId AND o.status = 'PAID'")
    Long countPaidOrdersBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Calculate total sales for a session (only PAID orders)
     */
    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o " +
            "WHERE o.session.id = :sessionId AND o.status = 'PAID'")
    BigDecimal calculateTotalSalesBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find recent transactions for a session
     */
    @Query("SELECT o FROM Order o " +
            "WHERE o.session.id = :sessionId AND o.status = 'PAID' " +
            "ORDER BY COALESCE(o.paidAt, o.createdAt) DESC")
    List<Order> findRecentTransactionsBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find all paid orders for a session
     */
    @Query("SELECT o FROM Order o WHERE o.session.id = :sessionId AND o.status = 'PAID'")
    List<Order> findPaidOrdersBySessionId(@Param("sessionId") Long sessionId);
}
