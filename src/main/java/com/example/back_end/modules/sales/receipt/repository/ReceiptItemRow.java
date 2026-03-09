package com.example.back_end.modules.sales.receipt.repository;

import java.math.BigDecimal;

/**
 * Lightweight projection for receipt items join (order_items + products).
 */
public interface ReceiptItemRow {
    Long getOrderItemId();

    String getSku();

    String getName();

    String getUnit();

    BigDecimal getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getLineTotal();
}
