package com.example.back_end.modules.cashier.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.back_end.modules.sales.payment.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

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
     * Find all payments for a session
     */
    @Query("SELECT p FROM Payment p WHERE p.order.session.id = :sessionId")
    List<Payment> findPaymentsBySessionId(@Param("sessionId") Long sessionId);
}