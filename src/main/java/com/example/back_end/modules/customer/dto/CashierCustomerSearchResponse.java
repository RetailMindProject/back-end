package com.example.back_end.modules.customer.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierCustomerSearchResponse {
    private boolean found;
    private CashierCustomerDTO customer;
}

