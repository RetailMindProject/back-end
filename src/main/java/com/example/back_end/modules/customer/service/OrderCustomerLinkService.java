package com.example.back_end.modules.customer.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.customer.dto.AttachByPhoneRequest;
import com.example.back_end.modules.customer.dto.CashierCustomerCreateRequest;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.register.entity.Customer;
import com.example.back_end.modules.register.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderCustomerLinkService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CashierCustomerService cashierCustomerService;

    @Transactional
    public AttachResult attachCustomerToOrder(Long orderId, Integer customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateAttachAllowed(order);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Idempotent: if already attached to same customer, no DB change for order.
        if (order.getCustomerId() != null && order.getCustomerId().equals(customerId.longValue())) {
            touchCustomerVisit(customer);
            customerRepository.save(customer);
            return new AttachResult(order.getId(), customerId, false);
        }

        order.setCustomerId(customerId.longValue());
        orderRepository.save(order);

        touchCustomerVisit(customer);
        customerRepository.save(customer);

        return new AttachResult(order.getId(), customerId, true);
    }

    @Transactional
    public AttachResult attachByPhone(Long orderId, AttachByPhoneRequest request) {
        String phone = request.getPhone();
        Customer customer = cashierCustomerService.searchByPhone(phone).orElse(null);

        if (customer == null) {
            if (!request.isCreateIfMissing()) {
                throw new ResourceNotFoundException("Customer not found");
            }
            if (request.getFirstName() == null || request.getFirstName().isBlank()) {
                throw new BusinessRuleException("firstName is required");
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new BusinessRuleException("email is required");
            }

            var created = cashierCustomerService.createCustomer(CashierCustomerCreateRequest.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .gender(null)
                    .birthDate(null)
                    .build());

            return attachCustomerToOrder(orderId, created.customer().getId());
        }

        return attachCustomerToOrder(orderId, customer.getId());
    }

    private static void validateAttachAllowed(Order order) {
        if (order.getStatus() != Order.OrderStatus.DRAFT && order.getStatus() != Order.OrderStatus.HOLD) {
            throw new BusinessRuleException("Order status does not allow attaching customer");
        }
    }

    private static void touchCustomerVisit(Customer customer) {
        customer.setLastVisitedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
    }

    public record AttachResult(Long orderId, Integer customerId, boolean attached) {}
}
