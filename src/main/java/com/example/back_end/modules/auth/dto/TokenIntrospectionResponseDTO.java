package com.example.back_end.modules.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenIntrospectionResponseDTO {
    private boolean valid;
    private Integer userId;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private Long expiresAt;  // Unix timestamp in milliseconds
}

