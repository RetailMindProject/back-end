package com.example.back_end.modules.customer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomerAttachRequest {
    @NotNull
    private Integer customerId;
}

