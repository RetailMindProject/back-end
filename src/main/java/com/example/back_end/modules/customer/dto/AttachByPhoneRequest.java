package com.example.back_end.modules.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachByPhoneRequest {

    @NotBlank
    @Size(max = 20)
    private String phone;

    private boolean createIfMissing;

    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @Email
    @Size(max = 120)
    private String email;
}

