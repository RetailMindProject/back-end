package com.example.back_end.modules.cashier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for closing current session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseSessionRequestDTO {
    private Double closingAmount;
}