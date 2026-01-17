package com.example.back_end.modules.customer.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierCustomerDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String gender;
    private LocalDate birthDate;
    private LocalDateTime lastVisitedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

