package com.example.back_end.modules.store_product.dto;

import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasteResponseDTO {

    private Long movementId;
    private Long productId;
    private String productName;
    private String sku;
    private Long batchId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    private BigDecimal wastedQuantity;
    private String wasteReason; // Extracted from note field
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private InventoryLocationType locationType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant wastedAt;
}

