package com.example.back_end.modules.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for session status information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusDTO {

    private Long sessionId;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Double openingFloat;
    private Double closingAmount;

    // Terminal info
    private Long terminalId;
    private String terminalCode;
    private String terminalDescription;

    // Session age info
    private Long hoursOpen;
    private boolean needsRotation; // true if > 24 hours
}