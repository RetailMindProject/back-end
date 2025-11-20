package com.example.back_end.modules.register.dto;

import com.example.back_end.modules.register.entity.User.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 60, message = "First name must not exceed 60 characters")
    private String firstName;

    @Size(max = 60, message = "Last name must not exceed 60 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Phone number is invalid")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    private String address;

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}