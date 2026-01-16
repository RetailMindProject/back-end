package com.example.back_end.modules.store_product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate; // optional, if provided, creates a batch with this expiration date

    @Size(max = 255)
    private String note;
}
