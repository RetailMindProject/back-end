package com.example.back_end.modules.store_product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasteRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long batchId; // Optional - required if product has batches

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotBlank(message = "Waste reason is required")
    private String note; // Store waste reason here: "EXPIRED", "BROKEN", "DAMAGED", etc.
}

