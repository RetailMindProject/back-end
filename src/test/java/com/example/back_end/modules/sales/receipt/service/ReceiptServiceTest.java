package com.example.back_end.modules.sales.receipt.service;

import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.OrderItemRepository;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.sales.receipt.repository.PaymentMethodSumRow;
import com.example.back_end.modules.sales.receipt.repository.ReceiptItemRow;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceiptServiceTest {

    @Test
    void generateReceiptPdf_shouldThrow409WhenOrderNotPaid() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        Order o = new Order();
        o.setId(1L);
        o.setOrderNumber("ORD-1");
        o.setStatus(Order.OrderStatus.DRAFT);
        o.setCreatedAt(LocalDateTime.now());
        o.setSubtotal(BigDecimal.TEN);
        o.setDiscountTotal(BigDecimal.ZERO);
        o.setTaxTotal(BigDecimal.ZERO);
        o.setGrandTotal(BigDecimal.TEN);

        when(orderRepository.findReceiptOrderById(1L)).thenReturn(Optional.of(o));

        ReceiptService service = new ReceiptService(orderRepository, orderItemRepository, paymentRepository);

        assertThatThrownBy(() -> service.generateReceiptPdf(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAID");
    }

    @Test
    void generateReceiptPdf_shouldGeneratePdfWhenPaid() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        Order o = new Order();
        o.setId(1L);
        o.setOrderNumber("ORD-1");
        o.setStatus(Order.OrderStatus.PAID);
        o.setCreatedAt(LocalDateTime.now());
        o.setPaidAt(LocalDateTime.now());
        o.setSubtotal(BigDecimal.TEN);
        o.setDiscountTotal(BigDecimal.ZERO);
        o.setTaxTotal(BigDecimal.ZERO);
        o.setGrandTotal(BigDecimal.TEN);

        when(orderRepository.findReceiptOrderById(1L)).thenReturn(Optional.of(o));

        ReceiptItemRow item = new ReceiptItemRow() {
            @Override public Long getOrderItemId() { return 100L; }
            @Override public String getSku() { return "SKU"; }
            @Override public String getName() { return "Test"; }
            @Override public String getUnit() { return "PCS"; }
            @Override public BigDecimal getQuantity() { return BigDecimal.ONE; }
            @Override public BigDecimal getUnitPrice() { return BigDecimal.TEN; }
            @Override public BigDecimal getLineTotal() { return BigDecimal.TEN; }
        };

        when(orderItemRepository.findReceiptItemsByOrderId(1L)).thenReturn(List.of(item));

        PaymentMethodSumRow cashRow = new PaymentMethodSumRow() {
            @Override public String getMethod() { return "CASH"; }
            @Override public BigDecimal getAmount() { return BigDecimal.TEN; }
        };
        when(paymentRepository.sumPaymentsByOrderIdGrouped(1L)).thenReturn(List.of(cashRow));

        ReceiptService service = new ReceiptService(orderRepository, orderItemRepository, paymentRepository);

        byte[] pdf = service.generateReceiptPdf(1L);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(10);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateReceiptPdf_shouldThrow404WhenNotFound() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);

        when(orderRepository.findReceiptOrderById(404L)).thenReturn(Optional.empty());

        ReceiptService service = new ReceiptService(orderRepository, orderItemRepository, paymentRepository);

        assertThatThrownBy(() -> service.generateReceiptPdf(404L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

