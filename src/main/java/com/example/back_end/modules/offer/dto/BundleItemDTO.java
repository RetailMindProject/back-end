package com.example.back_end.modules.offer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Required quantity is required")
    @DecimalMin(value = "0.01", message = "Required quantity must be greater than 0")
    private BigDecimal requiredQty;
}