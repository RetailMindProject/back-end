package com.example.back_end.modules.customer.mapper;

import com.example.back_end.modules.customer.dto.CashierCustomerDTO;
import com.example.back_end.modules.register.entity.Customer;

public final class CashierCustomerMapper {

    private CashierCustomerMapper() {}

    public static CashierCustomerDTO toDto(Customer c) {
        if (c == null) return null;
        return CashierCustomerDTO.builder()
                .id(c.getId() == null ? null : c.getId().longValue())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .gender(c.getGender() == null ? null : String.valueOf(c.getGender()))
                .birthDate(c.getBirthDate())
                .lastVisitedAt(c.getLastVisitedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

