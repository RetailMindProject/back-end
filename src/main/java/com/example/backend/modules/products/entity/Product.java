package com.example.back_end.modules.products.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"),
        indexes = {
                @Index(name = "idx_products_name", columnList = "name"),
                @Index(name = "idx_products_brand", columnList = "brand")
        }
)
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 64) @Column(length = 64)
    private String sku;

    @NotBlank @Size(max = 120) @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 80)  @Column(length = 80)
    private String brand;

    @Size(max = 500) @Column(length = 500)
    private String description;

    @PositiveOrZero
    @Column(name = "default_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal defaultCost;

    @PositiveOrZero
    @Column(name = "default_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal defaultPrice;

    @PositiveOrZero
    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Size(max = 20) @Column(length = 20)
    private String unit;

    @PositiveOrZero
    @Column(name = "wholesale_price", precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;

}
