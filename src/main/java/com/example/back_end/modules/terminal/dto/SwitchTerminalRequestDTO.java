package com.example.back_end.modules.terminal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for switching to a different terminal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchTerminalRequestDTO {

    @NotNull(message = "New terminal ID is required")
    private Long newTerminalId;

    @NotBlank(message = "Pairing code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pairing code must be 6 digits")
    private String pairingCode;
}