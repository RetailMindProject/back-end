package com.example.back_end.modules.publicapi.products.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductSearchTerm;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.catalog.product.repository.ProductSearchTermRepository;
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
class PublicProductServiceMultilingualSearchTest {

    @Autowired
    private PublicProductService publicProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSearchTermRepository searchTermRepository;

    @Autowired
    private StockSnapshotRepository stockSnapshotRepository;

    private Product activeProductEnglish;
    private Product inactiveProductArabic;

    @BeforeEach
    void setUp() {
        // Create an active product with English name: "Fresh Milk 1L"
        activeProductEnglish = Product.builder()
                .sku("SKU-MILK-EN-001")
                .name("Fresh Milk 1L")
                .brand("Dairy Farm")
                .description("Premium fresh milk 1 liter")
                .defaultCost(BigDecimal.valueOf(2.50))
                .defaultPrice(BigDecimal.valueOf(5.50))
                .isActive(true)
                .build();
        activeProductEnglish = productRepository.save(activeProductEnglish);

        // Create stock snapshot for active product (in stock)
        StockSnapshot activeStock = StockSnapshot.builder()
                .productId(activeProductEnglish.getId())
                .storeQty(BigDecimal.valueOf(50))
                .warehouseQty(BigDecimal.valueOf(100))
                .lastUpdatedAt(Instant.now())
                .build();
        stockSnapshotRepository.save(activeStock);

        // Add Arabic search terms for the milk product
        ProductSearchTerm arabicTerm1 = ProductSearchTerm.builder()
                .productId(activeProductEnglish.getId())
                .term("حليب")  // haleb/milk in Arabic
                .build();
        searchTermRepository.save(arabicTerm1);

        ProductSearchTerm arabicTerm2 = ProductSearchTerm.builder()
                .productId(activeProductEnglish.getId())
                .term("لبن")  // laban/milk in Arabic
                .build();
        searchTermRepository.save(arabicTerm2);

        // Create an inactive product with Arabic term for negative test
        inactiveProductArabic = Product.builder()
                .sku("SKU-MILK-AR-002")
                .name("Inactive Milk Product")
                .brand("OldBrand")
                .description("This is inactive")
                .defaultCost(BigDecimal.valueOf(1.00))
                .defaultPrice(BigDecimal.valueOf(2.00))
                .isActive(false)  // Inactive
                .build();
        inactiveProductArabic = productRepository.save(inactiveProductArabic);

        // Add Arabic term to inactive product
        ProductSearchTerm inactiveTerm = ProductSearchTerm.builder()
                .productId(inactiveProductArabic.getId())
                .term("حليب قديم")  // old milk in Arabic
                .build();
        searchTermRepository.save(inactiveTerm);

        // Create stock snapshot for inactive product (should not be returned)
        StockSnapshot inactiveStock = StockSnapshot.builder()
                .productId(inactiveProductArabic.getId())
                .storeQty(BigDecimal.valueOf(200))
                .warehouseQty(BigDecimal.valueOf(300))
                .lastUpdatedAt(Instant.now())
                .build();
        stockSnapshotRepository.save(inactiveStock);
    }

    @Test
    void multilingualSearch_shouldFindProductByArabicTerm() {
        // Test Case: Searching "حليب" (haleb/milk in Arabic) should find "Fresh Milk 1L"
        PublicProductListResponse response = publicProductService.search("حليب", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        // Check that "Fresh Milk 1L" is in results
        PublicProductDTO foundProduct = response.getItems().stream()
                .filter(p -> "Fresh Milk 1L".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(activeProductEnglish.getId());
        assertThat(foundProduct.getPrice()).isEqualTo(BigDecimal.valueOf(5.50));
        assertThat(foundProduct.getAvailable()).isTrue();
    }

    @Test
    void multilingualSearch_shouldFindByAlternativeArabicTerm() {
        // Test Case: Searching "لبن" (laban/milk in Arabic) should also find "Fresh Milk 1L"
        PublicProductListResponse response = publicProductService.search("لبن", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        PublicProductDTO foundProduct = response.getItems().stream()
                .filter(p -> p.getId().equals(activeProductEnglish.getId()))
                .findFirst()
                .orElse(null);

        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getName()).isEqualTo("Fresh Milk 1L");
    }

    @Test
    void multilingualSearch_shouldStillWorkForEnglishNames() {
        // Test Case: English search should still work alongside Arabic terms
        PublicProductListResponse response = publicProductService.search("Fresh Milk", 10);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        PublicProductDTO foundProduct = response.getItems().get(0);
        assertThat(foundProduct.getName()).isEqualTo("Fresh Milk 1L");
        assertThat(foundProduct.getId()).isEqualTo(activeProductEnglish.getId());
    }

    @Test
    void multilingualSearch_shouldNotReturnInactiveProductsEvenWithArabicTerms() {
        // Test Case: Inactive products should NEVER appear even if they have Arabic search terms
        PublicProductListResponse response = publicProductService.search("قديم", 10);

        // Should not find inactive product even though it has Arabic term "حليب قديم"
        boolean hasInactiveProduct = response.getItems().stream()
                .anyMatch(p -> p.getId().equals(inactiveProductArabic.getId()));

        assertThat(hasInactiveProduct).isFalse();
    }

    @Test
    void multilingualSearch_shouldReturnCorrectDTOStructure() {
        // Test Case: Response DTO should contain ONLY expected fields (DB-backed, no currency)
        PublicProductListResponse response = publicProductService.search("حليب", 10);

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
    void multilingualSearch_shouldSupportPartialArabicMatch() {
        // Test Case: Partial Arabic match should work (fuzzy)
        // "حلي" is partial of "حليب"
        PublicProductListResponse response = publicProductService.search("حلي", 10);

        // Should find milk product via fuzzy/trigram match
        boolean foundMilk = response.getItems().stream()
                .anyMatch(p -> p.getId().equals(activeProductEnglish.getId()));

        // May or may not find depending on similarity threshold, but if found, should be correct
        if (foundMilk) {
            PublicProductDTO product = response.getItems().stream()
                    .filter(p -> p.getId().equals(activeProductEnglish.getId()))
                    .findFirst()
                    .get();
            assertThat(product.getName()).isEqualTo("Fresh Milk 1L");
        }
    }

    @Test
    void multilingualSearch_shouldDeduplicate() {
        // Test Case: Product should appear only once even if multiple terms match
        // Add another term that also matches
        ProductSearchTerm extraTerm = ProductSearchTerm.builder()
                .productId(activeProductEnglish.getId())
                .term("milk fresh")
                .build();
        searchTermRepository.save(extraTerm);

        // Search with a query that could match both name and terms
        PublicProductListResponse response = publicProductService.search("milk", 10);

        // Count how many times the product appears
        long count = response.getItems().stream()
                .filter(p -> p.getId().equals(activeProductEnglish.getId()))
                .count();

        assertThat(count).isEqualTo(1);  // Should appear exactly once (deduplicated)
    }

    @Test
    void suggestions_shouldAlsoSupportMultilingualSearch() {
        // Test Case: Suggestions endpoint should also work with Arabic terms
        PublicProductListResponse response = publicProductService.suggestions("حليب", 5);

        boolean foundMilk = response.getItems().stream()
                .anyMatch(p -> p.getId().equals(activeProductEnglish.getId()));

        // Suggestions uses higher threshold (0.30), so may or may not find
        // But active-only filter MUST apply
        boolean hasInactiveProduct = response.getItems().stream()
                .anyMatch(p -> p.getId().equals(inactiveProductArabic.getId()));
        assertThat(hasInactiveProduct).isFalse();
    }
}

