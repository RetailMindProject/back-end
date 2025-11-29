package com.example.backend.modules.store_product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_snapshot")
public class StockSnapshot {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "store_qty", precision = 10, scale = 2, nullable = false)
    private BigDecimal storeQty;

    @Column(name = "warehouse_qty", precision = 10, scale = 2, nullable = false)
    private BigDecimal warehouseQty;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;
}
