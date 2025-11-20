package com.example.back_end.modules.register.dto;

import com.example.back_end.modules.register.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponseDTO {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String token;
    private String message;
}