package com.example.back_end.modules.sales.payment.repository;

import com.example.back_end.modules.sales.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ========================================
    // 🛒 POS Operations (من sales module)
    // ========================================

    /**
     * Find all payments for an order
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId ORDER BY p.createdAt")
    List<Payment> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Find payments by session
     */
    @Query("SELECT p FROM Payment p WHERE p.order.session.id = :sessionId ORDER BY p.createdAt")
    List<Payment> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Sum payments by method for session
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.session.id = :sessionId AND p.method = :method")
    BigDecimal sumBySessionAndMethod(@Param("sessionId") Long sessionId, @Param("method") Payment.PaymentMethod method);

    /**
     * Receipt summary: sum PAYMENTs by method for an order.
     */
    @Query(value = """
            SELECT p.method as method, COALESCE(SUM(p.amount), 0) as amount
            FROM payments p
            WHERE p.order_id = :orderId
              AND p.type = 'PAYMENT'
            GROUP BY p.method
            """, nativeQuery = true)
    List<com.example.back_end.modules.sales.receipt.repository.PaymentMethodSumRow> sumPaymentsByOrderIdGrouped(@Param("orderId") Long orderId);

    // ========================================
    // 💰 Cashier Operations (من cashier module)
    // ========================================

    /**
     * Calculate total CASH payments for a session
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.session.id = :sessionId " +
            "AND p.method = 'CASH' " +
            "AND p.order.status = 'PAID'")
    BigDecimal calculateCashInBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Calculate total CARD payments for a session
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.session.id = :sessionId " +
            "AND p.method = 'CARD' " +
            "AND p.order.status = 'PAID'")
    BigDecimal calculateCardInBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find all payments for a session (alias for findBySessionId)
     */
    @Query("SELECT p FROM Payment p WHERE p.order.session.id = :sessionId")
    List<Payment> findPaymentsBySessionId(@Param("sessionId") Long sessionId);

}