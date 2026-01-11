package com.example.back_end.modules.publicapi.products.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.publicapi.products.dto.PublicProductDTO;
import com.example.back_end.modules.publicapi.products.dto.PublicProductListResponse;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicProductServiceImpl implements PublicProductService {

    private final ProductRepository productRepository;
    private final StockSnapshotRepository stockSnapshotRepository;

    // Minimum similarity threshold for search (typo tolerance)
    private static final float SEARCH_MIN_SIMILARITY = 0.25f;
    // Higher threshold for suggestions (more strict)
    private static final float SUGGESTIONS_MIN_SIMILARITY = 0.30f;
    // Minimum query length to avoid noise
    private static final int MIN_QUERY_LENGTH = 2;

    @Override
    public PublicProductListResponse search(String query, int limit) {
        // Validate and normalize query
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return PublicProductListResponse.builder()
                    .items(List.of())
                    .total(0)
                    .build();
        }

        String normalizedQuery = query.trim();

        // Use fuzzy search with pg_trgm (typo-tolerant, active products only)
        List<Product> products = productRepository.fuzzySearch(
                normalizedQuery,
                SEARCH_MIN_SIMILARITY
        );

        // Apply limit
        int maxResults = Math.min(Math.max(limit, 1), 200);
        List<PublicProductDTO> items = products.stream()
                .limit(maxResults)
                .map(this::toPublicDTO)
                .collect(Collectors.toList());

        return PublicProductListResponse.builder()
                .items(items)
                .total(products.size())
                .build();
    }

    @Override
    public PublicProductDTO getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Product not found"));

        // Return 404 if product is inactive (customers should not see inactive products)
        if (!p.getIsActive()) {
            throw new jakarta.persistence.EntityNotFoundException("Product not found");
        }

        return toPublicDTO(p);
    }

    @Override
    public boolean isAvailable(Long id) {
        StockSnapshot s = stockSnapshotRepository.findById(id).orElse(null);
        BigDecimal store = s != null && s.getStoreQty() != null ? s.getStoreQty() : BigDecimal.ZERO;
        BigDecimal wh = s != null && s.getWarehouseQty() != null ? s.getWarehouseQty() : BigDecimal.ZERO;
        return store.add(wh).compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public PublicProductListResponse suggestions(String query, int limit) {
        // Validate and normalize query
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return PublicProductListResponse.builder()
                    .items(List.of())
                    .total(0)
                    .build();
        }

        String normalizedQuery = query.trim();

        // Use fuzzy search with higher threshold for suggestions (stricter)
        List<Product> products = productRepository.fuzzySearch(
                normalizedQuery,
                SUGGESTIONS_MIN_SIMILARITY
        );

        // Lower default limit for suggestions (typeahead use case)
        int maxResults = Math.min(Math.max(limit, 1), 50);
        List<PublicProductDTO> items = products.stream()
                .limit(maxResults)
                .map(this::toPublicDTO)
                .collect(Collectors.toList());

        return PublicProductListResponse.builder()
                .items(items)
                .total(products.size())
                .build();
    }

    private PublicProductDTO toPublicDTO(Product p) {
        StockSnapshot s = stockSnapshotRepository.findById(p.getId()).orElse(null);
        boolean available = false;
        if (s != null) {
            BigDecimal store = s.getStoreQty() != null ? s.getStoreQty() : BigDecimal.ZERO;
            BigDecimal wh = s.getWarehouseQty() != null ? s.getWarehouseQty() : BigDecimal.ZERO;
            available = store.add(wh).compareTo(BigDecimal.ZERO) > 0;
        }
        return PublicProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getDefaultPrice())
                .available(available)
                .build();
    }
}
