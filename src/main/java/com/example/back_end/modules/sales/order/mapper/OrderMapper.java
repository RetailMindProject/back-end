package com.example.back_end.modules.sales.order.mapper;

import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.sales.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Order entities and DTOs
 */
@Component
public class OrderMapper {

    /**
     * Map OrderItem to OrderItemResponse
     */
    public OrderDTO.OrderItemResponse toOrderItemResponse(OrderItem item) {
        // Calculate original line total (before discount)
        BigDecimal originalLineTotal = item.getUnitPrice()
                .multiply(item.getQuantity());

        OrderDTO.ProductMini productMini = null;
        if (item.getProduct() != null) {
            OrderDTO.ImageMini imageMini = item.getProduct().getProductMedia().stream()
                    .filter(pm -> pm.getMedia() != null)
                    .filter(pm -> Boolean.TRUE.equals(pm.getIsPrimary()))
                    .findFirst()
                    .map(pm -> OrderDTO.ImageMini.builder()
                            .url(toFrontendPath(pm.getMedia().getUrl()))
                            .altText(pm.getMedia().getAltText())
                            .build())
                    .orElse(null);

            productMini = OrderDTO.ProductMini.builder()
                    .id(item.getProduct().getId())
                    .name(item.getProduct().getName())
                    .image(imageMini)
                    .build();
        }

        return OrderDTO.OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .product(productMini)
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .discountAmount(item.getDiscountAmount())
                .lineTotal(item.getLineTotal())
                .offerId(item.getOfferId())
                .originalLineTotal(originalLineTotal)
                .build();
    }

    private static String toFrontendPath(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.startsWith("/")) return url;

        int idx = url.indexOf("//");
        if (idx >= 0) {
            int firstSlashAfterHost = url.indexOf('/', idx + 2);
            if (firstSlashAfterHost >= 0) {
                return url.substring(firstSlashAfterHost);
            }
        }

        return "/" + url;
    }

    /**
     * Map Payment to PaymentResponse
     */
    public OrderDTO.PaymentResponse toPaymentResponse(Payment payment) {
        return OrderDTO.PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getMethod().name())
                .amount(payment.getAmount())
                .paidAt(payment.getCreatedAt())
                .build();
    }

    /**
     * Map Order to OrderResponse (full details)
     */
    public OrderDTO.OrderResponse toOrderResponse(Order order, List<OrderItem> items, List<Payment> payments) {
        // Calculate totals
        BigDecimal amountPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal changeAmount = amountPaid.compareTo(order.getGrandTotal()) > 0
                ? amountPaid.subtract(order.getGrandTotal())
                : BigDecimal.ZERO;

        return OrderDTO.OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .sessionId(order.getSession().getId())
                .status(order.getStatus().name())
                .items(items.stream().map(this::toOrderItemResponse).collect(Collectors.toList()))
                .itemCount(items.size())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .grandTotal(order.getGrandTotal())
                .payments(payments.stream().map(this::toPaymentResponse).collect(Collectors.toList()))
                .amountPaid(amountPaid)
                .changeAmount(changeAmount)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }

    /**
     * Map Order to OrderSummary (for list/history)
     */
    public OrderDTO.OrderSummary toOrderSummary(Order order) {
        // Get payment methods if exists (for split payments, show both)
        String paymentMethod = null;
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            List<String> methods = order.getPayments().stream()
                    .map(p -> p.getMethod().name())
                    .distinct()
                    .collect(Collectors.toList());
            paymentMethod = methods.size() > 1 ? "SPLIT" : methods.get(0);
        }

        return OrderDTO.OrderSummary.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                .grandTotal(order.getGrandTotal())
                .paymentMethod(paymentMethod)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}