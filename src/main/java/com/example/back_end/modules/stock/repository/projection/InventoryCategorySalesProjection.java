package com.example.back_end.modules.stock.repository.projection;

import java.math.BigDecimal;

public interface InventoryCategorySalesProjection {

    String getCategoryName();
    BigDecimal getTotalSalesQty();
}
