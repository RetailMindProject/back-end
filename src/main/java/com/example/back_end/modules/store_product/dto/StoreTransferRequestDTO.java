package com.example.back_end.modules.store_product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreTransferRequestDTO {

    @NotNull
    private Long productId;

    @NotNull
    @Positive(message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @PositiveOrZero(message = "Unit cost must be >= 0")
    private BigDecimal unitCost; // optional, can be null

    @Size(max = 255)
    private String note;
}
