package com.example.back_end.modules.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for customer order history.
 * Returns list of orders with items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrdersResponseDTO {
    private List<CustomerOrderDTO> orders;
    private Integer total;
    private Integer limit;
    private Boolean hasMore;

    /**
     * Individual order details for customer view.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderDTO {
        private Long id;
        private String orderNumber;
        private String status;
        private BigDecimal subtotal;
        private BigDecimal discountTotal;
        private BigDecimal taxTotal;
        private BigDecimal grandTotal;
        private Integer itemCount;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private List<CustomerOrderItemDTO> items;
    }

    /**
     * Order item details for customer view.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerOrderItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BigDecimal discountAmount;
    }
}

