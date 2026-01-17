package com.example.back_end.modules.sales.receipt.repository;

import java.math.BigDecimal;

/**
 * Lightweight projection for payment sums grouped by method.
 */
public interface PaymentMethodSumRow {
    String getMethod();

    BigDecimal getAmount();
}

