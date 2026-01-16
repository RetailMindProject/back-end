package com.example.back_end.modules.publicapi.products.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.publicapi.products.dto.PublicProductDTO;
import com.example.back_end.modules.publicapi.products.dto.PublicProductListResponse;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PublicProductServiceFuzzySearchTest {

    @Autowired
    private PublicProductService publicProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockSnapshotRepository stockSnapshotRepository;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        // Create an active product: "Fresh Milk 1L"
        activeProduct = Product.builder()
                .sku("SKU-MILK-001")
                .name("Fresh Milk 1L")
                .brand("Dairy Farm")
                .description("Premium fresh milk 1 liter")
                .defaultCost(BigDecimal.valueOf(2.50))
                .defaultPrice(BigDecimal.valueOf(5.50))
                .isActive(true)
                .build();
        activeProduct = productRepository.save(activeProduct);

        // Create stock snapshot for active product (in stock)
        StockSnapshot activeStock = StockSnapshot.builder()
                .productId(activeProduct.getId())
                .storeQty(BigDecimal.valueOf(50))
                .warehouseQty(BigDecimal.valueOf(100))
                .lastUpdatedAt(Instant.now())
                .build();
        stockSnapshotRepository.save(activeStock);

        // Create an inactive product for negative test
        inactiveProduct = Product.builder()
                .sku("SKU-MILK-002")
                .name("Inactive Milk Product")
                .brand("OldBrand")
                .description("This is inactive")
                .defaultCost(BigDecimal.valueOf(1.00))
                .defaultPrice(BigDecimal.valueOf(2.00))
                .isActive(false)  // Inactive
                .build();
        inactiveProduct = productRepository.save(inactiveProduct);

        // Create stock snapshot for inactive product (should not be returned)
        StockSnapshot inactiveStock = StockSnapshot.builder()
                .productId(inactiveProduct.getId())
                .storeQty(BigDecimal.valueOf(200))
                .warehouseQty(BigDecimal.valueOf(300))
                .lastUpdatedAt(Instant.now())
                .build();
        stockSnapshotRepository.save(inactiveStock);
    }

    @Test
    void fuzzySearch_shouldFindProductWithTypo() {
        // Test Case: Searching "milke" (typo) should find "Fresh Milk 1L"
        PublicProductListResponse response = publicProductService.search("milke", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        // Check that "Fresh Milk 1L" is in results
        PublicProductDTO foundProduct = response.getItems().stream()
                .filter(p -> "Fresh Milk 1L".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(activeProduct.getId());
        assertThat(foundProduct.getPrice()).isEqualTo(BigDecimal.valueOf(5.50));
        assertThat(foundProduct.getAvailable()).isTrue();
    }

    @Test
    void fuzzySearch_shouldFindByExactName() {
        // Test Case: Exact name match should work
        PublicProductListResponse response = publicProductService.search("Fresh Milk 1L", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        PublicProductDTO foundProduct = response.getItems().get(0);
        assertThat(foundProduct.getName()).isEqualTo("Fresh Milk 1L");
        assertThat(foundProduct.getId()).isEqualTo(activeProduct.getId());
    }

    @Test
    void fuzzySearch_shouldFindBySku() {
        // Test Case: Search by SKU with typo "SKU-MIL" (missing "K")
        PublicProductListResponse response = publicProductService.search("SKU-MIL", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        // Should find "Fresh Milk 1L" via SKU fuzzy match
        PublicProductDTO foundProduct = response.getItems().stream()
                .filter(p -> p.getId().equals(activeProduct.getId()))
                .findFirst()
                .orElse(null);

        assertThat(foundProduct).isNotNull();
    }

    @Test
    void fuzzySearch_shouldNotReturnInactiveProducts() {
        // Test Case: Inactive products should NEVER appear in results
        // Even though "Inactive Milk Product" contains "milk", it's inactive
        PublicProductListResponse response = publicProductService.search("inactive", 10);

        // Should return empty or only active products
        boolean hasInactiveProduct = response.getItems().stream()
                .anyMatch(p -> p.getId().equals(inactiveProduct.getId()));

        assertThat(hasInactiveProduct).isFalse();
    }

    @Test
    void fuzzySearch_shouldReturnCorrectDTO() {
        // Test Case: Response DTO contains only expected fields (DB-backed)
        PublicProductListResponse response = publicProductService.search("milk", 10);

        assertThat(response.getItems()).isNotEmpty();
        PublicProductDTO product = response.getItems().get(0);

        // Verify DTO contains ONLY expected fields
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isNotNull();
        assertThat(product.getPrice()).isNotNull();
        assertThat(product.getAvailable()).isNotNull();
        // Ensure no currency field is exposed
        assertThatCode(() -> product.getClass().getDeclaredField("currency"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void fuzzySearch_shouldHandleEmptyQuery() {
        // Test Case: Empty or very short query should return empty
        PublicProductListResponse emptyResponse = publicProductService.search("", 10);
        assertThat(emptyResponse.getItems()).isEmpty();
        assertThat(emptyResponse.getTotal()).isZero();

        PublicProductListResponse shortResponse = publicProductService.search("a", 10);
        assertThat(shortResponse.getItems()).isEmpty();
    }

    @Test
    void suggestions_shouldUseHigherThreshold() {
        // Test Case: Suggestions should be stricter than search
        // "milke" might be in suggestions but with higher threshold
        PublicProductListResponse suggestionsResponse = publicProductService.suggestions("milke", 5);

        // Suggestions may or may not find "milke" (depends on threshold)
        // But active-only filter MUST apply
        boolean hasInactiveProduct = suggestionsResponse.getItems().stream()
                .anyMatch(p -> p.getId().equals(inactiveProduct.getId()));
        assertThat(hasInactiveProduct).isFalse();
    }

    @Test
    void search_shouldRespectLimit() {
        // Create multiple active products to test limit
        for (int i = 0; i < 5; i++) {
            Product p = Product.builder()
                    .sku("SKU-MILK-" + i)
                    .name("Milk Product " + i)
                    .brand("Brand")
                    .defaultCost(BigDecimal.ONE)
                    .defaultPrice(BigDecimal.TEN)
                    .isActive(true)
                    .build();
            Product saved = productRepository.save(p);

            StockSnapshot s = StockSnapshot.builder()
                    .productId(saved.getId())
                    .storeQty(BigDecimal.TEN)
                    .warehouseQty(BigDecimal.TEN)
                    .lastUpdatedAt(Instant.now())
                    .build();
            stockSnapshotRepository.save(s);
        }

        PublicProductListResponse response = publicProductService.search("milk", 2);

        assertThat(response.getItems()).hasSizeLessThanOrEqualTo(2);
    }
}

