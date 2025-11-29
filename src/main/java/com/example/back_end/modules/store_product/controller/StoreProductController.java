package com.example.backend.modules.store_product.controller;

import com.example.backend.modules.store_product.dto.StoreProductResponseDTO;
import com.example.backend.modules.store_product.dto.StoreTransferRequestDTO;
import com.example.backend.modules.store_product.service.StoreProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store-products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreProductController {

    private final StoreProductService service;

    // Transfer WAREHOUSE → STORE
    @PostMapping("/transfer-to-store")
    public ResponseEntity<StoreProductResponseDTO> transferToStore(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.transferFromInventoryToStore(dto));
    }

    // Transfer STORE → WAREHOUSE
    @PostMapping("/transfer-to-inventory")
    public ResponseEntity<StoreProductResponseDTO> transferToInventory(
            @Valid @RequestBody StoreTransferRequestDTO dto) {

        return ResponseEntity.ok(service.transferFromStoreToInventory(dto));
    }

    // Search store products only
    @GetMapping
    public ResponseEntity<List<StoreProductResponseDTO>> search(
            @RequestParam(required = false) String q) {

        return ResponseEntity.ok(service.search(q));
    }

    // Get stock for a specific product
    @GetMapping("/{productId}")
    public ResponseEntity<StoreProductResponseDTO> getByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(service.getByProductId(productId));
    }
}
