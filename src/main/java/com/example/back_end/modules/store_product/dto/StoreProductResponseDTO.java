package com.example.back_end.modules.store_product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProductResponseDTO {

    private Long productId;
    private String sku;
    private String name;

    private BigDecimal storeQty;
    private BigDecimal warehouseQty;

    private BigDecimal defaultPrice;
    private BigDecimal defaultCost;

    private Instant lastUpdatedAt;
}
