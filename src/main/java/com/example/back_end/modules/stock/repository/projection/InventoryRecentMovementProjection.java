package com.example.back_end.modules.stock.repository.projection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InventoryRecentMovementProjection {

    LocalDateTime getMovedAt();
    String getProductName();
    String getCategoryName();
    String getLocationType();
    String getRefType();
    BigDecimal getQuantityChange();
}

