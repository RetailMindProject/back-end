package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;

public interface InventorySummaryProjection {

    BigDecimal getTotalIn();

    BigDecimal getTotalOut();

    Long getMovementCount();
}