package com.example.back_end.modules.terminal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTOs for Terminal Management
 * Used by Store Manager to manage terminals
 */
public class TerminalManagementDTO {

    /**
     * Request to create a new terminal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Terminal code is required")
        @Size(max = 30, message = "Terminal code must not exceed 30 characters")
        private String code;

        @Size(max = 100, message = "Description must not exceed 100 characters")
        private String description;
    }

    /**
     * Request to update terminal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 30, message = "Terminal code must not exceed 30 characters")
        private String code;

        @Size(max = 100, message = "Description must not exceed 100 characters")
        private String description;

        private Boolean isActive;
    }

    /**
     * Terminal response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminalResponse {
        private Long id;
        private String code;
        private String description;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime lastSeenAt;
    }
}

