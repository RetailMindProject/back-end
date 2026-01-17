package com.example.back_end.modules.customer.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierCustomerCreateResponse {
    private Integer userId;
    private Integer customerId;
    private CashierCustomerDTO customer;
}

