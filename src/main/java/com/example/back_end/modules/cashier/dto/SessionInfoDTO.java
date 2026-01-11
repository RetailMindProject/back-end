package com.example.back_end.modules.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for complete session information with terminal details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDTO {

    // Session details
    private Long sessionId;
    private String status;
    private LocalDateTime openedAt;
    private Double openingFloat;
    private Long userId;
    private String userName;

    // Terminal details
    private Long terminalId;
    private String terminalCode;
    private String terminalDescription;

    // Browser pairing info
    private boolean isPaired;
    private LocalDateTime pairedAt;

    // Additional info
    private String message;
}