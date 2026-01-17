package com.example.back_end.modules.sales.receipt.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReceiptData(
        Header header,
        List<ItemLine> items,
        Totals totals,
        PaymentSummary payments
) {

    @Builder
    public record Header(
            String storeName,
            String storePhone,
            String orderNumber,
            LocalDateTime paidAt
    ) {
    }

    @Builder
    public record ItemLine(
            String sku,
            String name,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    @Builder
    public record Totals(
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal
    ) {
    }

    @Builder
    public record PaymentSummary(
            BigDecimal cash,
            BigDecimal card
    ) {
        public boolean isMixed() {
            return cash != null && card != null
                    && cash.compareTo(BigDecimal.ZERO) > 0
                    && card.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}

