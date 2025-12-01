package com.example.back_end.modules.register.dto;

import com.example.back_end.modules.register.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequestDTO {

    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Phone number is invalid")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    private String address;

    private User.UserRole role;

    private Boolean isActive;
}
