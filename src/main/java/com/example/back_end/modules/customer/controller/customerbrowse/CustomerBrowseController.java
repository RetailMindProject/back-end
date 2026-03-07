package com.example.back_end.modules.customer.controller.customerbrowse;

import com.example.back_end.modules.customer.dto.customerbrowse.*;
import com.example.back_end.modules.customer.service.customerbrowse.CategoryCustomerService;
import com.example.back_end.modules.customer.service.customerbrowse.ProductCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerBrowseController {

    private final CategoryCustomerService categoryCustomerService;
    private final ProductCustomerService productCustomerService;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('CUSTOMER','CASHIER','STORE_MANAGER')")
    public ResponseEntity<List<CategoryRootDTO>> getRootCategories() {
        return ResponseEntity.ok(categoryCustomerService.getRootCategories());
    }

    @GetMapping("/categories/{id}/children")
    @PreAuthorize("hasAnyRole('CUSTOMER','CASHIER','STORE_MANAGER')")
    public ResponseEntity<List<CategoryChildDTO>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(categoryCustomerService.getChildren(id));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('CUSTOMER','CASHIER','STORE_MANAGER')")
    public ResponseEntity<PageResponseDTO<ProductListItemDTO>> getProducts(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "offersOnly", defaultValue = "false") boolean offersOnly,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "sortKey", required = false) String sortKey,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(productCustomerService.getProducts(categoryId, q, offersOnly, minPrice, maxPrice, sortKey, page, size));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','CASHIER','STORE_MANAGER')")
    public ResponseEntity<ProductDetailsDTO> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productCustomerService.getProductDetails(id));
    }
}
