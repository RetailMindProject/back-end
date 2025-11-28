package com.example.back_end.modules.messages.dto;

import com.example.back_end.modules.register.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
}
