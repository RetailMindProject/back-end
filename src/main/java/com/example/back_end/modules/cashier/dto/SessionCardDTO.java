package com.example.back_end.modules.cashier.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Session Cards in Sessions List Page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCardDTO {

    // From users table
    private Integer cashierId;
    private String firstName;
    private String lastName;
    private String email;

    // From sessions table
    private Long sessionId;
    private LocalDateTime openedAt;
    private String status; // OPEN or CLOSED

    // Calculated from orders table
    private Long ordersCount;      // COUNT(orders.id)
    private BigDecimal totalSales; // SUM(orders.grand_total) for PAID orders only
}
