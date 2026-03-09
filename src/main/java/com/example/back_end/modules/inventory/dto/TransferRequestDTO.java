package com.example.back_end.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TransferRequestDTO {

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull
        private Long productId;

        @NotNull
        @Positive
        private BigDecimal quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private String fromLocation; // must be WAREHOUSE

        @NotNull
        private String toLocation; // must be STORE

        @NotEmpty
        @Valid
        private List<Item> items;

        @Size(max = 255)
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        @NotNull
        @Size(min = 3, max = 500)
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long requestId; // stored as Message.id
        private RequestStatus status;
        private Integer requestedByUserId;
        private LocalDateTime requestedAt;
        private Integer decidedByUserId;
        private LocalDateTime decidedAt;
        private String decisionReason; // for REJECTED
        private String note;
        private String requesterName;
        private List<Item> items;
    }
}

