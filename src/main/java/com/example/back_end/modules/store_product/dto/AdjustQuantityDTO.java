package com.example.back_end.modules.store_product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustQuantityDTO {

    @NotNull
    private Long productId;

    @NotNull
    @Positive(message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @Size(max = 255)
    private String note;
}

