package com.example.back_end.modules.cashier.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for closing a cashier session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseSessionRequest {

    /**
     * Closing amount (optional)
     * The amount of cash in the drawer when closing the session
     */
    @DecimalMin(value = "0.0", message = "Closing amount must be greater than or equal to 0")
    private BigDecimal closingAmount;
}

