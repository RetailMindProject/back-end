package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public interface WasteHistoryProjection {

    Long getMovementId();
    Long getProductId();
    String getProductName();
    String getSku();
    Long getBatchId();
    LocalDate getExpirationDate();
    BigDecimal getQuantity();
    String getWasteReason(); // From note field (deprecated, use getNote)
    String getNote(); // Note field from inventory_movements
    BigDecimal getUnitCost();
    BigDecimal getTotalCost();
    String getLocationType();
    Instant getWastedAt();
}

