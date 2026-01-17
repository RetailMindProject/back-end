package com.example.back_end.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password reset confirmation (step 2: set new password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetConfirmRequest {

    /**
     * Password reset token from the email link.
     */
    @NotBlank(message = "Token is required")
    private String token;

    /**
     * New password.
     * Must meet security requirements:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String newPassword;

    /**
     * Confirmation of the new password (must match newPassword).
     */
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}

