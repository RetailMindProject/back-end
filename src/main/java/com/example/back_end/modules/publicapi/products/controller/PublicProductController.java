package com.example.back_end.modules.publicapi.products.controller;

import com.example.back_end.modules.publicapi.products.dto.PublicProductDTO;
import com.example.back_end.modules.publicapi.products.dto.PublicProductListResponse;
import com.example.back_end.modules.publicapi.products.service.PublicProductService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class PublicProductController {

    private final PublicProductService service;

    // 1) Search
    @GetMapping("/search")
    public ResponseEntity<PublicProductListResponse> search(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) int limit) {
        return ResponseEntity.ok(service.search(query, Math.min(limit, 200)));
    }

    // 2) Details by ID
    @GetMapping("/{id}")
    public ResponseEntity<PublicProductDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // 3) Availability
    @GetMapping("/{id}/availability")
    public ResponseEntity<?> availability(@PathVariable("id") Long id) {
        boolean available = service.isAvailable(id);
        return ResponseEntity.ok(java.util.Map.of("id", id, "available", available));
    }

    // 4) Suggestions (optional - autocomplete use case)
    @GetMapping("/suggestions")
    public ResponseEntity<PublicProductListResponse> suggestions(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "limit", defaultValue = "5") @Min(1) int limit) {
        return ResponseEntity.ok(service.suggestions(query, Math.min(limit, 50)));
    }
}

