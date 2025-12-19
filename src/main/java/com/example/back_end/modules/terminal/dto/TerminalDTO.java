package com.example.back_end.modules.terminal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TerminalDTO {

    /**
     * Request to open a new cashier session
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenSessionRequest {
        @NotNull(message = "Terminal ID is required")
        private Long terminalId;

        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull(message = "Opening float is required")
        @PositiveOrZero(message = "Opening float must be zero or positive")
        private BigDecimal openingFloat;
    }

    /**
     * Request to close a cashier session
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CloseSessionRequest {
        @NotNull(message = "Closing amount is required")
        @PositiveOrZero(message = "Closing amount must be zero or positive")
        private BigDecimal closingAmount;
    }

    /**
     * Response after opening/closing session
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionResponse {
        private Long sessionId;
        private Long terminalId;
        private String terminalCode;
        private Long userId;
        private String userName;
        private LocalDateTime openedAt;
        private LocalDateTime closedAt;
        private BigDecimal openingFloat;
        private BigDecimal closingAmount;
        private String status;
    }

    /**
     * Terminal info for selection screen
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminalInfo {
        private Long id;
        private String code;
        private String description;
        private Boolean isActive;
        private Boolean hasActiveSession;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastSessionInfo {
        private Long lastSessionId;
        private LocalDateTime closedAt;
        private BigDecimal closingAmount;
        private String terminalCode;
        private String message;  // رسالة للكاشير
    }

    }