package com.example.back_end.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for email verification request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationRequest {

    /**
     * Verification token from the email link.
     */
    private String token;
}

