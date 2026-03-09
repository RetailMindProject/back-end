package com.example.back_end.modules.catalog.product.dto;

import lombok.*;
import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSimpleDTO {
    private Long id;
    private String sku;
    private String name;

    /** Mini primary image (path-only url) for POS UI lists. */
    private ProductImageMiniDTO image;

    private BigDecimal defaultPrice;
    private BigDecimal taxRate;
    private String unit;
}