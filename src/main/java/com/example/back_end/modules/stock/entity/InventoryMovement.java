package com.example.back_end.modules.stock.entity;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // integer في الداتابيس، Long هنا مقبول

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private InventoryLocationType locationType; // WAREHOUSE / STORE

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 20)
    private InventoryRefType refType; // PURCHASE / SALE / RETURN / ...

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "qty_change", nullable = false, precision = 10, scale = 2)
    private BigDecimal qtyChange; // موجبة = IN، سالبة = OUT

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "moved_at")
    private LocalDateTime movedAt;

    @Column(name = "note", columnDefinition = "text")
    private String note;
}