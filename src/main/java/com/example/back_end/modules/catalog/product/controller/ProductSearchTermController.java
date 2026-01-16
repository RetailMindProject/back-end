package com.example.back_end.modules.catalog.product.controller;

import com.example.back_end.modules.catalog.product.dto.ProductSearchTermResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductSearchTermsDTO;
import com.example.back_end.modules.catalog.product.service.ProductSearchTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductSearchTermController {

    private final ProductSearchTermService searchTermService;

    /**
     * Add/replace search terms for a product (multilingual aliases)
     * Requires CEO or STORE_MANAGER role
     */
    @PostMapping("/{productId}/search-terms")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<List<ProductSearchTermResponseDTO>> updateSearchTerms(
            @PathVariable Long productId,
            @Valid @RequestBody ProductSearchTermsDTO dto) {
        List<ProductSearchTermResponseDTO> terms = searchTermService.updateSearchTerms(productId, dto);
        return ResponseEntity.ok(terms);
    }

    /**
     * Get all search terms for a product
     * Requires CEO or STORE_MANAGER role
     */
    @GetMapping("/{productId}/search-terms")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<List<ProductSearchTermResponseDTO>> getSearchTerms(@PathVariable Long productId) {
        List<ProductSearchTermResponseDTO> terms = searchTermService.getSearchTerms(productId);
        return ResponseEntity.ok(terms);
    }

    /**
     * Delete a specific search term
     * Requires CEO or STORE_MANAGER role
     */
    @DeleteMapping("/{productId}/search-terms/{termId}")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<Void> deleteSearchTerm(
            @PathVariable Long productId,
            @PathVariable Long termId) {
        searchTermService.deleteSearchTerm(productId, termId);
        return ResponseEntity.noContent().build();
    }
}

