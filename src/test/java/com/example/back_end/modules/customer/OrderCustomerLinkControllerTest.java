package com.example.back_end.modules.customer;

import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.customer.dto.AttachByPhoneRequest;
import com.example.back_end.modules.customer.service.CashierCustomerService;
import com.example.back_end.modules.customer.service.OrderCustomerLinkService;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.repository.CustomerRepository;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCustomerLinkControllerTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    CashierCustomerService cashierCustomerService;

    @InjectMocks
    OrderCustomerLinkService service;

    @BeforeEach
    void setup() {
        // default noop
    }

    @Test
    void attach_success_draft() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-1");
        order.setStatus(Order.OrderStatus.DRAFT);
        order.setSession(new Session());

        Customer customer = new Customer();
        customer.setId(5);
        customer.setFirstName("Ahmad");
        customer.setLastName("Saleh");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(5)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.attachCustomerToOrder(10L, 5);

        assertThat(result.customerId()).isEqualTo(5);
        assertThat(order.getCustomerId()).isEqualTo(5L);
    }

    @Test
    void attach_rejected_paid() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(Order.OrderStatus.PAID);
        order.setSession(new Session());

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.attachCustomerToOrder(10L, 5))
                .isInstanceOf(com.example.back_end.exception.BusinessRuleException.class)
                .hasMessageContaining("Order status");
    }

    @Test
    void attachByPhone_createAndAttach() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(Order.OrderStatus.HOLD);
        order.setSession(new Session());

        AttachByPhoneRequest req = AttachByPhoneRequest.builder()
                .phone("0777777777")
                .createIfMissing(true)
                .firstName("New")
                .email("new@test.com")
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        when(cashierCustomerService.searchByPhone("0777777777")).thenReturn(Optional.empty());

        Customer created = new Customer();
        created.setId(7);
        created.setFirstName("New");
        created.setLastName("Customer");
        when(cashierCustomerService.createCustomer(any())).thenReturn(
                new CashierCustomerService.CreatedCustomer(new com.example.back_end.modules.register.entity.User(), created)
        );

        when(customerRepository.findById(7)).thenReturn(Optional.of(created));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.attachByPhone(10L, req);

        assertThat(result.customerId()).isEqualTo(7);
        assertThat(result.attached()).isTrue();
        assertThat(order.getCustomerId()).isEqualTo(7L);
    }

    @Test
    void attachByPhone_notFound_whenCreateDisabled() {
        Order order = new Order();
        order.setId(10L);
        order.setStatus(Order.OrderStatus.HOLD);
        order.setSession(new Session());

        AttachByPhoneRequest req = AttachByPhoneRequest.builder()
                .phone("0777777777")
                .createIfMissing(false)
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(cashierCustomerService.searchByPhone("0777777777")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachByPhone(10L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }
}
