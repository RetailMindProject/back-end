package com.example.back_end.modules.sales.payment.entity;

import com.example.back_end.modules.sales.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ========================================
    // Timestamps
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ========================================
    // 🆕 Helper method for Service compatibility
    // ========================================

    /**
     * Alias for createdAt (to match Service code that uses paidAt)
     */
    @Transient
    public LocalDateTime getPaidAt() {
        return createdAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        // No-op: createdAt is auto-generated
        // Or you can set it if needed for testing
    }

    /**
     * Alias for method (to match Service code that uses paymentMethod)
     */
    @Transient
    public PaymentMethod getPaymentMethod() {
        return method;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.method = paymentMethod;
    }

    // ========================================
    // Enum
    // ========================================

    public enum PaymentMethod {
        CASH,
        CARD
    }
}