package com.example.back_end.modules.customer.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomerAttachResponse {
    private Long orderId;
    private Integer customerId;
    private boolean attached;

    /** Minimal customer display data for the frontend. */
    private CustomerDisplay customer;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDisplay {
        private String firstName;
        private String lastName;
        private String customerName;
    }
}
