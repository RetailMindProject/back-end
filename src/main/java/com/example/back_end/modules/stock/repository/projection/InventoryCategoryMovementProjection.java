package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;

public interface InventoryCategoryMovementProjection {

    String getCategoryName();
    BigDecimal getTotalIn();
    BigDecimal getTotalOut();
}
