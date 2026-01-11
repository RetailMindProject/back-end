package com.example.back_end.modules.catalog.product.service;

import com.example.back_end.modules.catalog.product.dto.ProductSearchTermResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductSearchTermsDTO;
import com.example.back_end.modules.catalog.product.entity.ProductSearchTerm;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.catalog.product.repository.ProductSearchTermRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSearchTermService {

    private final ProductSearchTermRepository searchTermRepository;
    private final ProductRepository productRepository;

    /**
     * Add/replace search terms for a product
     * Deletes existing terms and adds new ones (full replacement)
     */
    @Transactional
    public List<ProductSearchTermResponseDTO> updateSearchTerms(Long productId, ProductSearchTermsDTO dto) {
        // Validate product exists
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        // Delete existing terms
        searchTermRepository.deleteByProductId(productId);
        searchTermRepository.flush();

        // Add new terms (deduplicate, trim, ignore empty)
        List<ProductSearchTerm> savedTerms = new ArrayList<>();
        if (dto.getTerms() != null) {
            List<String> uniqueTerms = dto.getTerms().stream()
                    .filter(term -> term != null && !term.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            for (String term : uniqueTerms) {
                ProductSearchTerm searchTerm = ProductSearchTerm.builder()
                        .productId(productId)
                        .term(term)
                        .build();
                savedTerms.add(searchTermRepository.save(searchTerm));
            }
        }

        return savedTerms.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all search terms for a product
     */
    @Transactional(readOnly = true)
    public List<ProductSearchTermResponseDTO> getSearchTerms(Long productId) {
        // Validate product exists
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        return searchTermRepository.findByProductIdOrderByTermAsc(productId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delete a specific search term
     */
    @Transactional
    public void deleteSearchTerm(Long productId, Long termId) {
        // Validate product exists
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        // Validate term exists and belongs to product
        ProductSearchTerm term = searchTermRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Search term not found: " + termId));

        if (!term.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Search term does not belong to this product");
        }

        searchTermRepository.delete(term);
    }

    private ProductSearchTermResponseDTO toDTO(ProductSearchTerm entity) {
        return ProductSearchTermResponseDTO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .term(entity.getTerm())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

