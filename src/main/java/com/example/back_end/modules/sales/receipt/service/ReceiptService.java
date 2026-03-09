package com.example.back_end.modules.sales.receipt.service;

import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.OrderItemRepository;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.sales.receipt.dto.ReceiptData;
import com.example.back_end.modules.sales.receipt.repository.PaymentMethodSumRow;
import com.example.back_end.modules.sales.receipt.repository.ReceiptItemRow;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    private final ReceiptPdfRenderer pdfRenderer = new ReceiptPdfRenderer();

    @Transactional(readOnly = true)
    public byte[] generateReceiptPdf(Long orderId) {
        Order order = orderRepository.findReceiptOrderById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.PAID) {
            throw new IllegalStateException("Receipt can only be generated for PAID orders");
        }

        List<ReceiptItemRow> itemRows = orderItemRepository.findReceiptItemsByOrderId(orderId);

        ReceiptData.PaymentSummary paymentSummary = loadPaymentSummary(orderId);

        LocalDateTime paidAt = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();

        ReceiptData receiptData = ReceiptData.builder()
                .header(ReceiptData.Header.builder()
                        .storeName("My Store")
                        .storePhone("+962-000-0000")
                        .orderNumber(order.getOrderNumber())
                        .paidAt(paidAt)
                        .build())
                .items(itemRows.stream().map(r -> ReceiptData.ItemLine.builder()
                        .sku(r.getSku())
                        .name(r.getName())
                        .unit(r.getUnit())
                        .quantity(r.getQuantity())
                        .unitPrice(r.getUnitPrice())
                        .lineTotal(r.getLineTotal())
                        .build()).toList())
                .totals(ReceiptData.Totals.builder()
                        .subtotal(order.getSubtotal())
                        .discountTotal(order.getDiscountTotal())
                        .taxTotal(order.getTaxTotal())
                        .grandTotal(order.getGrandTotal())
                        .build())
                .payments(paymentSummary)
                .build();

        return pdfRenderer.render(receiptData);
    }

    private ReceiptData.PaymentSummary loadPaymentSummary(Long orderId) {
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;

        List<PaymentMethodSumRow> rows = paymentRepository.sumPaymentsByOrderIdGrouped(orderId);
        for (PaymentMethodSumRow row : rows) {
            if (row.getMethod() == null) {
                continue;
            }
            switch (row.getMethod()) {
                case "CASH" -> cash = nz(row.getAmount());
                case "CARD" -> card = nz(row.getAmount());
            }
        }

        return ReceiptData.PaymentSummary.builder()
                .cash(cash)
                .card(card)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
