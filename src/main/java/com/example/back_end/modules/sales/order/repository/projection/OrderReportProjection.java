package com.example.back_end.modules.sales.order.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderReportProjection {
    Long getOrderId();
    String getOrderNumber();
    LocalDateTime getCreatedAt();
    LocalDateTime getPaidAt();
    Long getSessionId();
    Long getCashierId();
    String getCashierFirstName();
    String getCashierLastName();
    Integer getCustomerId();
    String getCustomerFirstName();
    String getCustomerLastName();
    String getCustomerPhone();
    String getStatus();
    Integer getItemCount();
    BigDecimal getSubtotal();
    BigDecimal getDiscountAmount();
    BigDecimal getTaxAmount();
    BigDecimal getGrandTotal();
    String getPaymentMethod(); // Will be calculated: SPLIT, CASH, CARD, or UNKNOWN
    Integer getPaymentCount(); // Count of payments for this order
}
