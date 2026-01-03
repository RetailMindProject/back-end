package com.example.back_end.modules.terminal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating a pairing code
 * Used by administrators
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePairingCodeRequestDTO {

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    @Min(value = 1, message = "Validity minutes must be at least 1")
    private Integer validityMinutes = 60; // Default 1 hour
}