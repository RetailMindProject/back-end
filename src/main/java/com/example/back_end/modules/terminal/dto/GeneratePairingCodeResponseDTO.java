package com.example.back_end.modules.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO after generating a pairing code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePairingCodeResponseDTO {

    private Long terminalId;
    private String terminalCode;
    private String pairingCode; // Plain code (6 digits) - shown only once
    private LocalDateTime expiresAt;
    private Integer validityMinutes;
    private String message;
}