package com.example.back_end.modules.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for current terminal information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTerminalDTO {

    private Long terminalId;
    private String terminalCode;
    private String terminalDescription;
    private boolean isActive;
    private LocalDateTime pairedAt;
    private LocalDateTime lastSeenAt;

    // Current session info
    private Long currentSessionId;
    private String sessionStatus;
    private LocalDateTime sessionOpenedAt;
    private Double openingFloat;
}