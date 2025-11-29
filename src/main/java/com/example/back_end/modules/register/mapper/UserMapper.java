package com.example.back_end.modules.register.mapper;

import com.example.back_end.modules.register.dto.RegisterResponseDTO;
import com.example.back_end.modules.register.dto.UserDTO;
import com.example.back_end.modules.register.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public RegisterResponseDTO toRegisterResponseDTO(User user, String token) {
        if (user == null) {
            return null;
        }

        return RegisterResponseDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .token(token)
                .message("User registered successfully")
                .build();
    }
}