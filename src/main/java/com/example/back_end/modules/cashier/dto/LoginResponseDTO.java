package com.example.back_end.modules.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO after login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token; // JWT token
    private Long userId;
    private String username;
    private String role;

    // Terminal pairing status
    private boolean isPaired;
    private Long terminalId;
    private String terminalCode;

    // Session info (if paired)
    private Long sessionId;
    private String sessionStatus;
    private Double openingFloat;

    private String message;
}