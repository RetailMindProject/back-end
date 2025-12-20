package com.example.back_end.modules.cashier.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for filtering sessions in Sessions List Page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFilterDTO {

    private String cashierName;  // Search by cashier name
    private LocalDate date;      // Filter by date
    private LocalTime time;      // Filter by time
    private String status;       // Filter by status: ACTIVE, CLOSED, ALL
}
