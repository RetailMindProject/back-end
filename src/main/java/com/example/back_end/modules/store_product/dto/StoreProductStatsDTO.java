package com.example.back_end.modules.store_product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProductStatsDTO {
    private Long totalProducts;
    private Long lowStock;
    private Long outOfStock;
}
