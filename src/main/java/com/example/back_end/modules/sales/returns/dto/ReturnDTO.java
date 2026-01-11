package com.example.back_end.modules.sales.returns.dto;

import com.example.back_end.modules.sales.payment.entity.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

public class ReturnDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReturnRequest {

        @NotNull
        private Long originalOrderId;

        @NotNull
        private Long sessionId;

        @NotEmpty
        @Valid
        private List<ReturnItemRequest> items;

        @NotEmpty
        @Valid
        private List<RefundRequest> refunds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {

        @NotNull
        private Long originalOrderItemId;

        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        private BigDecimal returnedQty;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {

        @NotNull
        private Payment.PaymentMethod method;

        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        private BigDecimal amount;
    }

    // ==========================
    // Response
    // ==========================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnResponse {
        private Long returnOrderId;
        private Long originalOrderId;
        private Long customerId; // nullable
        private BigDecimal totalRefund;
        private List<ReturnedItemResponse> items;
        private List<RefundResponse> refunds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnedItemResponse {
        private Long originalOrderItemId;
        private BigDecimal returnedQty;
        private BigDecimal refundAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundResponse {
        private Payment.PaymentMethod method;
        private BigDecimal amount;
    }
}

