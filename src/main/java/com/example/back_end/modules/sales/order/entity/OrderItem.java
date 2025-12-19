package com.example.back_end.modules.sales.order.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // ========================================
    // 🆕 Product info at time of sale (added)
    // ========================================





    // ========================================
    // Quantities and prices (modified)
    // ========================================

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_discount", precision = 12, scale = 2)
    private BigDecimal lineDiscount = BigDecimal.ZERO;  // ✅ Keep this name (from DB)

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;



    @Column(name = "offer_id")
    private Long offerId;




    /**
     * Alias for lineDiscount (to match Service code)
     */
    @Transient
    public BigDecimal getDiscountAmount() {
        return lineDiscount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.lineDiscount = discountAmount;
    }
}