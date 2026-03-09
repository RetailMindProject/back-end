package com.example.back_end.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password reset request (step 1: request reset token).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {

    /**
     * Email address of the user requesting password reset.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}

