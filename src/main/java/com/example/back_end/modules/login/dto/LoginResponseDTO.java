package com.example.back_end.modules.login.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private Integer userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String token;
    private String redirectUrl;
    private String message;
    private boolean success;
}