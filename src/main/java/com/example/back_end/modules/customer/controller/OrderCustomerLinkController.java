package com.example.back_end.modules.customer.controller;

import com.example.back_end.modules.customer.dto.AttachByPhoneRequest;
import com.example.back_end.modules.customer.dto.OrderCustomerAttachRequest;
import com.example.back_end.modules.customer.dto.OrderCustomerAttachResponse;
import com.example.back_end.modules.customer.service.CashierCustomerService;
import com.example.back_end.modules.customer.service.OrderCustomerLinkService;
import com.example.back_end.modules.register.entity.Customer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderCustomerLinkController {

    private final OrderCustomerLinkService orderCustomerLinkService;
    private final CashierCustomerService cashierCustomerService;

    @PutMapping("/{orderId}/customer")
    @PreAuthorize("hasAnyRole('CASHIER','STORE_MANAGER')")
    public ResponseEntity<OrderCustomerAttachResponse> attachCustomerToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderCustomerAttachRequest request
    ) {
        var result = orderCustomerLinkService.attachCustomerToOrder(orderId, request.getCustomerId());
        Customer customer = cashierCustomerService.getById(result.customerId());

        return ResponseEntity.ok(OrderCustomerAttachResponse.builder()
                .orderId(result.orderId())
                .customerId(result.customerId())
                .attached(result.attached())
                .customer(toDisplay(customer))
                .build());
    }

    @PostMapping("/{orderId}/customer/attach-by-phone")
    @PreAuthorize("hasAnyRole('CASHIER','STORE_MANAGER')")
    public ResponseEntity<OrderCustomerAttachResponse> attachByPhone(
            @PathVariable Long orderId,
            @Valid @RequestBody AttachByPhoneRequest request
    ) {
        var result = orderCustomerLinkService.attachByPhone(orderId, request);
        Customer customer = cashierCustomerService.getById(result.customerId());

        return ResponseEntity.ok(OrderCustomerAttachResponse.builder()
                .orderId(result.orderId())
                .customerId(result.customerId())
                .attached(result.attached())
                .customer(toDisplay(customer))
                .build());
    }

    private static OrderCustomerAttachResponse.CustomerDisplay toDisplay(Customer c) {
        String first = c == null ? null : c.getFirstName();
        String last = c == null ? null : c.getLastName();
        String full = null;
        if (first != null && last != null) full = (first + " " + last).trim();
        else if (first != null) full = first;
        else if (last != null) full = last;

        return OrderCustomerAttachResponse.CustomerDisplay.builder()
                .firstName(first)
                .lastName(last)
                .customerName(full)
                .build();
    }
}
