package com.example.back_end.modules.dashboard.storedashboard.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface StoreWeeklySalesProjection {
    LocalDate getSaleDate();
    BigDecimal getRevenue();
    Long getOrders();
}

