package com.example.back_end.modules.customer.controller;

import com.example.back_end.modules.customer.dto.CashierCustomerCreateRequest;
import com.example.back_end.modules.customer.dto.CashierCustomerCreateResponse;
import com.example.back_end.modules.customer.dto.CashierCustomerSearchResponse;
import com.example.back_end.modules.customer.mapper.CashierCustomerMapper;
import com.example.back_end.modules.customer.service.CashierCustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CashierCustomerController {

    private final CashierCustomerService cashierCustomerService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CASHIER','STORE_MANAGER')")
    public ResponseEntity<CashierCustomerSearchResponse> searchByPhone(@RequestParam("phone") String phone) {
        return cashierCustomerService.searchByPhone(phone)
                .map(c -> ResponseEntity.ok(
                        CashierCustomerSearchResponse.builder()
                                .found(true)
                                .customer(CashierCustomerMapper.toDto(c))
                                .build()
                ))
                .orElseGet(() -> ResponseEntity.ok(
                        CashierCustomerSearchResponse.builder()
                                .found(false)
                                .build()
                ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CASHIER','STORE_MANAGER')")
    public ResponseEntity<CashierCustomerCreateResponse> createCustomer(@Valid @RequestBody CashierCustomerCreateRequest request) {
        var created = cashierCustomerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CashierCustomerCreateResponse.builder()
                .userId(created.user().getId())
                .customerId(created.customer().getId())
                .customer(CashierCustomerMapper.toDto(created.customer()))
                .build());
    }
}
