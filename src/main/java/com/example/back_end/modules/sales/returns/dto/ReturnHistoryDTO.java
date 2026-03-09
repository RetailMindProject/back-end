package com.example.back_end.modules.sales.returns.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Returns History APIs.
 */
public class ReturnHistoryDTO {

    /**
     * A) Summary (list) of original orders that have at least one return.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnedOrderSummary {
        private Long orderId;
        private String orderNumber;
        private LocalDateTime orderDate;

        /** Nullable - project may not always resolve customer name */
        private String customerName;

        /** Total paid for the original order (grand_total snapshot). */
        private BigDecimal totalPaid;

        /** How many return orders exist for this original order. */
        private Long returnCount;

        /** Total refunded amount across all return orders (sum of return orders grand_total). */
        private BigDecimal totalReturned;

        /** Latest return order created_at (or paid_at if you prefer). */
        private LocalDateTime lastReturnAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnedOrdersPage {
        private List<ReturnedOrderSummary> items;
        private long total;
        private int limit;
        private int offset;
    }

    /**
     * B) Return order summary for a particular original order.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnOrderSummary {
        private Long returnOrderId;
        private String returnOrderNumber;
        private LocalDateTime createdAt;
        private BigDecimal totalRefund;
        private Long itemCount;
    }

    /**
     * C) Full return details (same shape as ReturnDTO.ReturnResponse for reuse).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnDetails {
        private Long returnOrderId;
        private Long originalOrderId;
        private Long customerId;
        private String customerName;
        private BigDecimal totalRefund;
        private List<ReturnDTO.ReturnedItemResponse> items;
        private List<ReturnDTO.RefundResponse> refunds;
        private LocalDateTime createdAt;
    }
}
