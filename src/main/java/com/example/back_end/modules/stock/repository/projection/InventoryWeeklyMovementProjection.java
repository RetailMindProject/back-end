package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface InventoryWeeklyMovementProjection {

    LocalDate getMovementDate();
    BigDecimal getTotalIn();
    BigDecimal getTotalOut();
}
