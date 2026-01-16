package com.example.back_end.modules.store_product.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBatchDTO {

    private Long batchId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    private BigDecimal totalQuantity;
}

