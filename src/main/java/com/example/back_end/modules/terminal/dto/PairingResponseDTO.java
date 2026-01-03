package com.example.back_end.modules.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO after successful pairing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairingResponseDTO {

    private Long terminalId;
    private String terminalCode;
    private String terminalDescription;
    private Long sessionId;
    private String sessionStatus;
    private Double openingFloat;
    private String message;
}