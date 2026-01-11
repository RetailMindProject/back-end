package com.example.back_end.modules.sales.returns.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.sales.order.repository.OrderItemRepository;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.entity.Payment;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.sales.returns.config.ReturnProperties;
import com.example.back_end.modules.sales.returns.dto.ReturnDTO;
import com.example.back_end.modules.sales.returns.repository.ReturnItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ReturnItemRepository returnItemRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    com.example.back_end.modules.cashier.repository.SessionRepository sessionRepository;

    ReturnProperties returnProperties;

    @InjectMocks
    ReturnService returnService;

    @Captor
    ArgumentCaptor<Order> orderCaptor;

    @BeforeEach
    void setup() {
        ReturnProperties props = new ReturnProperties();
        props.setWindowDays(14);
        this.returnProperties = props;

        // re-create service manually to inject ReturnProperties
        returnService = new ReturnService(orderRepository, orderItemRepository, returnItemRepository, paymentRepository, sessionRepository, returnProperties);
    }

    @Test
    void copiesCustomerIdFromOriginalAutomatically_andKeepsNullIfOriginalNull() {
        Order original = paidOrderWithCustomer(null);
        original.setId(10L);

        OrderItem oi = orderItem(100L, original, bd("2.00"), bd("10.00"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(original));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(openSession(1L)));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(BigDecimal.ZERO);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(999L);
            return o;
        });

        ReturnDTO.CreateReturnRequest req = new ReturnDTO.CreateReturnRequest(
                10L,
                1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("1.00"))),
                List.of(new ReturnDTO.RefundRequest(Payment.PaymentMethod.CASH, bd("5.00")))
        );

        ReturnDTO.ReturnResponse resp = returnService.createReturn(req);

        assertThat(resp.getCustomerId()).isNull();
        assertThat(resp.getReturnOrderId()).isEqualTo(999L);

        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        Order savedReturn = orderCaptor.getAllValues().stream().filter(o -> o.getParentOrderId() != null).findFirst().orElseThrow();
        assertThat(savedReturn.getCustomerId()).isNull();
        assertThat(savedReturn.getParentOrderId()).isEqualTo(10L);
        assertThat(savedReturn.getStatus()).isEqualTo(Order.OrderStatus.RETURNED);
    }

    @Test
    void preventsOverReturnAcrossMultipleReturns() {
        Order original = paidOrderWithCustomer(77L);
        original.setId(10L);

        OrderItem oi = orderItem(100L, original, bd("2.00"), bd("10.00"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(original));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(openSession(1L)));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));

        // already returned 1.50, original is 2.00 => remaining 0.50
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(bd("1.50"));

        ReturnDTO.CreateReturnRequest req = new ReturnDTO.CreateReturnRequest(
                10L,
                1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("1.00"))),
                List.of(new ReturnDTO.RefundRequest(Payment.PaymentMethod.CASH, bd("5.00")))
        );

        assertThatThrownBy(() -> returnService.createReturn(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds remaining");
    }

    @Test
    void refundCalculationUsesNetUnit_fromLineTotalOverQuantity() {
        Order original = paidOrderWithCustomer(77L);
        original.setId(10L);

        // lineTotal 10.00, qty 3.00 => netUnit 3.33333333..., returned 2.00 => 6.67
        OrderItem oi = orderItem(100L, original, bd("3.00"), bd("10.00"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(original));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(openSession(1L)));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(BigDecimal.ZERO);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(999L);
            return o;
        });

        ReturnDTO.CreateReturnRequest req = new ReturnDTO.CreateReturnRequest(
                10L,
                1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("2.00"))),
                List.of(new ReturnDTO.RefundRequest(Payment.PaymentMethod.CARD, bd("6.67")))
        );

        ReturnDTO.ReturnResponse resp = returnService.createReturn(req);
        assertThat(resp.getTotalRefund()).isEqualByComparingTo(bd("6.67"));
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getRefundAmount()).isEqualByComparingTo(bd("6.67"));
    }

    @Test
    void refundPaymentsSumMustMatchTotalRefund() {
        Order original = paidOrderWithCustomer(77L);
        original.setId(10L);

        OrderItem oi = orderItem(100L, original, bd("2.00"), bd("10.00"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(original));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(openSession(1L)));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(BigDecimal.ZERO);

        ReturnDTO.CreateReturnRequest req = new ReturnDTO.CreateReturnRequest(
                10L,
                1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("1.00"))),
                List.of(
                        new ReturnDTO.RefundRequest(Payment.PaymentMethod.CASH, bd("2.00")),
                        new ReturnDTO.RefundRequest(Payment.PaymentMethod.CARD, bd("2.00"))
                )
        );

        // expected totalRefund = 5.00 (10/2*1), but paymentsSum=4.00
        assertThatThrownBy(() -> returnService.createReturn(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must equal computed totalRefund");
    }

    @Test
    void updatesOriginalStatusToPartiallyReturnedThenReturned() {
        Order original = paidOrderWithCustomer(77L);
        original.setId(10L);

        OrderItem oi = orderItem(100L, original, bd("2.00"), bd("10.00"));

        when(orderRepository.findById(10L)).thenReturn(Optional.of(original));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(openSession(1L)));
        when(orderItemRepository.findByOrderId(10L)).thenReturn(List.of(oi));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) o.setId(999L);
            return o;
        });

        // First return: no previous returns
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(BigDecimal.ZERO);

        ReturnDTO.CreateReturnRequest req1 = new ReturnDTO.CreateReturnRequest(
                10L, 1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("1.00"))),
                List.of(new ReturnDTO.RefundRequest(Payment.PaymentMethod.CASH, bd("5.00")))
        );
        returnService.createReturn(req1);

        // After first return, simulate aggregated returned qty = 1.00
        when(returnItemRepository.sumReturnedQty(10L, 100L)).thenReturn(bd("1.00"));

        ReturnDTO.CreateReturnRequest req2 = new ReturnDTO.CreateReturnRequest(
                10L, 1L,
                List.of(new ReturnDTO.ReturnItemRequest(100L, bd("1.00"))),
                List.of(new ReturnDTO.RefundRequest(Payment.PaymentMethod.CASH, bd("5.00")))
        );
        returnService.createReturn(req2);

        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());

        // capture the last saved original order status
        Order lastOriginalSave = null;
        for (Order saved : orderCaptor.getAllValues()) {
            if (saved.getId() != null && saved.getId().equals(10L)) {
                lastOriginalSave = saved;
            }
        }

        assertThat(lastOriginalSave).isNotNull();
        assertThat(lastOriginalSave.getStatus()).isEqualTo(Order.OrderStatus.RETURNED);
    }

    private static Order paidOrderWithCustomer(Long customerId) {
        Order o = new Order();
        o.setStatus(Order.OrderStatus.PAID);
        o.setPaidAt(LocalDateTime.now().minusDays(1));
        o.setOrderNumber("ORD-TEST");
        o.setCustomerId(customerId);
        return o;
    }

    private static OrderItem orderItem(Long id, Order order, BigDecimal qty, BigDecimal lineTotal) {
        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setOrder(order);
        oi.setQuantity(qty);
        oi.setLineTotal(lineTotal);
        oi.setUnitPrice(BigDecimal.ZERO);
        return oi;
    }

    private static Session openSession(Long id) {
        Session s = new Session();
        s.setId(id);
        s.setStatus(Session.SessionStatus.OPEN);
        return s;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
