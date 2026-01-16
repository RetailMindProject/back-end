package com.example.back_end.modules.sales.order.entity;

import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.sales.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_session_id", columnList = "session_id"),
        @Index(name = "idx_orders_order_number", columnList = "order_number"),
        @Index(name = "idx_orders_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OrderStatus status = OrderStatus.DRAFT;



    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;



    @Column(name = "tax_total", precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ========================================
    // Timestamps
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Legacy scalar column kept for compatibility with existing code/DB.
     * Prefer using parentOrder association when working with returns.
     */
    @Column(name = "parent_order_id")
    private Long parentOrderId;

    /**
     * Return orders are linked to the original order via orders.parent_order_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id", insertable = false, updatable = false)
    private Order parentOrder;

    /**
     * Customer is auto-copied from the original order during return flow.
     * This project doesn't currently have a Customer entity, so we map as read-only id.
     */
    @Column(name = "customer_id")
    private Long customerId;

    // ========================================
    // Relationships
    // ========================================

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    // ========================================
    // 🔥 Helper methods for Service compatibility
    // ========================================

    /**
     * Alias for discountTotal (Service uses discountAmount)
     */
    @Transient
    public BigDecimal getDiscountAmount() {
        return discountTotal;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountTotal = discountAmount;
    }

    /**
     * Alias for taxTotal (Service uses taxAmount)
     */
    @Transient
    public BigDecimal getTaxAmount() {
        return taxTotal;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxTotal = taxAmount;
    }

    // ========================================
    // Enum
    // ========================================

    public enum OrderStatus {
        DRAFT,      // New order being created
        HOLD,       // Saved for later
        PAID,       // Payment completed
        CANCELLED,  // Cancelled/voided
        RETURNED,   // Returned/refunded
        PARTIALLY_RETURNED // Some items returned
    }
}