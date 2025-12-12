package com.example.back_end.modules.catalog.product.dto;
import com.example.back_end.modules.catalog.category.dto.CategorySimpleDTO; 

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;
    private String sku;
    private String name;
    private String brand;
    private String description;
    private BigDecimal defaultCost;
    private BigDecimal defaultPrice;
    private BigDecimal taxRate;
    private String unit;
    private BigDecimal wholesalePrice;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    
    private List<CategorySimpleDTO> categories;


}