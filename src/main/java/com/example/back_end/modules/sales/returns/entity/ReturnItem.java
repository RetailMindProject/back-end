package com.example.back_end.modules.sales.returns.entity;

import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_return_items_per_order", columnNames = {"return_order_id", "original_order_item_id"})
}, indexes = {
        @Index(name = "idx_return_items_return_order", columnList = "return_order_id"),
        @Index(name = "idx_return_items_original_item", columnList = "original_order_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_order_id", nullable = false)
    private Order returnOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_order_item_id", nullable = false)
    private OrderItem originalOrderItem;

    @Column(name = "returned_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal returnedQty;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

