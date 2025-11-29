package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;

public interface InventoryTopProductProjection {

    Long getProductId();
    String getProductName();
    String getCategoryName();
    BigDecimal getTotalMovementQty(); // SUM(ABS(qty_change))
}
