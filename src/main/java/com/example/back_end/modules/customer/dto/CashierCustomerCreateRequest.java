package com.example.back_end.modules.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashierCustomerCreateRequest {

    @NotBlank
    @Size(max = 60)
    private String firstName;

    @Size(max = 60)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    private String phone;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    // optional
    @Pattern(regexp = "^[MF]$", message = "gender must be M or F")
    private String gender;

    // optional
    private LocalDate birthDate;
}
