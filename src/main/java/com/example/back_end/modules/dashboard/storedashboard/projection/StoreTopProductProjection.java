package com.example.back_end.modules.dashboard.storedashboard.projection;

import java.math.BigDecimal;

public interface StoreTopProductProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    BigDecimal getSold();
    BigDecimal getRevenue();
}

