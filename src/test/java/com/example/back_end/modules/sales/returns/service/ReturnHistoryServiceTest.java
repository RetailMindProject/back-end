package com.example.back_end.modules.sales.returns.service;

import com.example.back_end.modules.sales.returns.dto.ReturnHistoryDTO;
import com.example.back_end.modules.sales.returns.repository.ReturnHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnHistoryServiceTest {

    @Mock
    ReturnHistoryRepository returnHistoryRepository;

    @Mock
    com.example.back_end.modules.sales.order.repository.OrderRepository orderRepository;

    @Mock
    com.example.back_end.modules.sales.returns.repository.ReturnItemRepository returnItemRepository;

    @Mock
    com.example.back_end.modules.sales.payment.repository.PaymentRepository paymentRepository;

    @Mock
    com.example.back_end.modules.register.repository.CustomerRepository customerRepository;

    @InjectMocks
    ReturnHistoryService returnHistoryService;

    @Test
    void listReturnedOrders_mapsProjectionToDto_whenFiltersAreNull() {
        ReturnHistoryRepository.ReturnedOrderSummaryRow row = new ReturnHistoryRepository.ReturnedOrderSummaryRow() {
            @Override public Long getOrderId() { return 10L; }
            @Override public String getOrderNumber() { return "ORD-10"; }
            @Override public LocalDateTime getOrderDate() { return LocalDateTime.now().minusDays(1); }
            @Override public String getCustomerName() { return "Ahmad Saleh"; }
            @Override public BigDecimal getTotalPaid() { return new BigDecimal("100.00"); }
            @Override public Long getReturnCount() { return 2L; }
            @Override public BigDecimal getTotalReturned() { return new BigDecimal("30.00"); }
            @Override public LocalDateTime getLastReturnAt() { return LocalDateTime.now().minusHours(2); }
        };

        Page<ReturnHistoryRepository.ReturnedOrderSummaryRow> page = new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1);

        when(returnHistoryRepository.findReturnedOrders(isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        ReturnHistoryDTO.ReturnedOrdersPage result = returnHistoryService.listReturnedOrders(10, 0, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getOrderId()).isEqualTo(10L);
        assertThat(result.getItems().get(0).getOrderNumber()).isEqualTo("ORD-10");
        assertThat(result.getItems().get(0).getCustomerName()).isEqualTo("Ahmad Saleh");
        assertThat(result.getItems().get(0).getReturnCount()).isEqualTo(2L);
        assertThat(result.getItems().get(0).getTotalReturned()).isEqualByComparingTo("30.00");
    }
}
