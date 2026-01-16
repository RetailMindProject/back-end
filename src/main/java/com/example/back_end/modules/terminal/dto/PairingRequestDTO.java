package com.example.back_end.modules.terminal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTOs for terminal pairing operations
 * Supports both old flow (direct pairing) and new flow (request-approval)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairingRequestDTO {

    // ========================================
    // OLD FLOW - Direct pairing with code
    // ========================================

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    @NotBlank(message = "Pairing code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pairing code must be 6 digits")
    private String pairingCode;

    private Boolean forceOverride; // null or false = ask first, true = override

    // ========================================
    // NEW FLOW - Request-Approval workflow
    // ========================================

    /**
     * Cashier: Create pairing request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "Terminal ID is required")
        private Long terminalId;
    }

    /**
     * Response for pairing request status
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long terminalId;
        private String terminalCode;
        private String terminalDescription;
        private Long requestedBy;
        private String requestedByName;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        private String status; // PENDING, APPROVED, USED, EXPIRED, REJECTED
        private Long approvedBy;
        private String approvedByName;
        private LocalDateTime approvedAt;
        private String message;
    }

    /**
     * Manager: Reject request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        private String reason; // Not stored in DB, just for logging
    }
}