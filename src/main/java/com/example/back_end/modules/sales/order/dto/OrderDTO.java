package com.example.back_end.modules.sales.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {

    /**
     * Request to create new order
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private Long sessionId;
    }

    /**
     * Request to add item to order
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        private BigDecimal quantity;

        @PositiveOrZero(message = "Discount cannot be negative")
        private BigDecimal discountAmount;
    }

    /**
     * Request to update item quantity (delta)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateItemRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
    }

    /**
     * Request to apply discount to order
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplyDiscountRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;

        @PositiveOrZero(message = "Discount cannot be negative")
        private BigDecimal discountAmount;

        @PositiveOrZero(message = "Discount percentage cannot be negative")
        private BigDecimal discountPercentage;

        private String discountReason;
    }

    /**
     * Request to process payment
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;

        @NotNull(message = "Payment method is required")
        private String paymentMethod;

        private BigDecimal amount;
        private BigDecimal cashAmount;
        private BigDecimal cardAmount;
    }

    /**
     * Order item response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;

        /** Minimal product payload for UI convenience (cart thumbnails). */
        private ProductMini product;

        private BigDecimal unitPrice;
        private BigDecimal quantity;
        private BigDecimal discountAmount;
        private BigDecimal lineTotal;
        private Long offerId;
        private String offerTitle;
        private BigDecimal originalLineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMini {
        private Long id;
        private String name;
        private ImageMini image;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageMini {
        private String url;
        private String altText;
    }

    /**
     * Payment response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long id;
        private String paymentMethod;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }

    /**
     * Full order response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private Long id;
        private String orderNumber;
        private Long sessionId;
        private String status;
        private String customerName;
        private String customerPhone;
        private List<OrderItemResponse> items;
        private Integer itemCount;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal taxAmount;
        private BigDecimal grandTotal;
        private List<PaymentResponse> payments;
        private BigDecimal amountPaid;
        private BigDecimal changeAmount;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private String notes;
    }

    /**
     * Order summary for history list
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private Long id;
        private String orderNumber;
        private String status;
        private String customerName;
        private Integer itemCount;
        private BigDecimal grandTotal;
        private String paymentMethod;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
    }
}