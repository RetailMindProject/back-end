package com.example.back_end.modules.cashier.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Cashier Details Page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierDetailsDTO {

    // A) Cashier Information (from users table)
    private CashierInfo cashierInfo;

    // B) Session Information (from sessions table)
    private SessionInfo sessionInfo;

    // C) Performance (calculated from orders + payments)
    private Performance performance;

    // D) Recent Transactions (from orders table)
    private List<RecentTransaction> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashierInfo {
        private Integer cashierId;
        private String name;        // firstName + lastName
        private String email;
        private String phone;
        private String role;        // CASHIER
        private Boolean active;     // is_active
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Long sessionId;
        private LocalDateTime openedAt;
        private BigDecimal openingFloat;
        private BigDecimal closingAmount; // nullable
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Performance {
        private Long totalOrders;      // Count of PAID orders
        private BigDecimal totalSales; // Sum of grand_total for PAID orders
        private BigDecimal cashIn;     // Sum of payments where method = CASH
        private BigDecimal cardIn;     // Sum of payments where method = CARD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransaction {
        private String orderNumber;
        private LocalDateTime time;    // paid_at or created_at
        private BigDecimal amount;     // grand_total
    }
}
