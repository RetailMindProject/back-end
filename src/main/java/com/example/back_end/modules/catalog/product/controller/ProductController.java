package com.example.back_end.modules.catalog.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.back_end.modules.catalog.product.dto.*;
import com.example.back_end.modules.catalog.product.service.ProductService;
import com.example.back_end.modules.catalog.product.dto.ProductStatsDTO;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService service;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeStock,
            @RequestParam(required = false, defaultValue = "false") boolean includeCategories) {
        return ResponseEntity.ok(service.getById(id, includeStock, includeCategories));
    }

    // Simple search by name or SKU
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponseDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return ResponseEntity.ok(service.search(q, pageable));
    }

    // Advanced filter with sorting
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponseDTO>> filter(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer minWarehouseQuantity,
            @RequestParam(required = false) Integer minStoreQuantity,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean outOfStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean includeStock,
            @RequestParam(required = false, defaultValue = "false") boolean includeCategories) {
        // Parse sort parameter
        Sort sortObj = null;
        if (sort != null && !sort.trim().isEmpty()) {
            // Handle formats: "warehouseQuantity_desc", "warehouseQuantity,desc", "sales_asc", etc.
            String sortField;
            Sort.Direction direction = Sort.Direction.DESC;
            
            if (sort.contains("_")) {
                // Format: warehouseQuantity_desc or sales_asc
                String[] parts = sort.split("_", 2);
                sortField = parts[0];
                direction = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else if (sort.contains(",")) {
                // Format: warehouseQuantity,desc
                String[] parts = sort.split(",", 2);
                sortField = parts[0];
                direction = "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else {
                // Default: assume desc
                sortField = sort;
            }
            
            // Map sort fields to database columns (snake_case for native queries)
            String dbColumn = mapSortFieldToColumn(sortField);
            if (dbColumn != null) {
                sortObj = Sort.by(direction, dbColumn);
            }
        }
        
        // Default sort if not specified (use snake_case for native queries)
        if (sortObj == null) {
            sortObj = Sort.by(Sort.Direction.DESC, "created_at");
        }
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResponseEntity.ok(service.filter(brand, isActive, minPrice, maxPrice, sku, 
                minWarehouseQuantity, minStoreQuantity, search, lowStock, outOfStock, pageable, includeStock, includeCategories));
    }
    
    private String mapSortFieldToColumn(String sortField) {
        // Map frontend sort fields to database columns (snake_case for native queries)
        // Note: Special fields like warehouseQuantity and sales are handled in service layer
        switch (sortField.toLowerCase()) {
            case "warehousequantity":
            case "warehouse_quantity":
            case "warehouseqty":
                return "warehouseQty"; // Special handling in service layer
            case "warehousequantity_with_zero_last":
            case "warehouse_quantity_with_zero_last":
            case "warehouseqty_with_zero_last":
                return "warehouseQty_with_zero_last"; // Special handling in service layer
            case "sales":
            case "orders":
                return "sales"; // Special handling in service layer
            case "created_at":
            case "createdat":
                return "created_at"; // Database column name (snake_case)
            case "name":
                return "name"; // Database column name
            case "price":
            case "defaultprice":
                return "default_price"; // Database column name (snake_case)
            default:
                // For unknown fields, try to convert camelCase to snake_case
                // or use as-is if already snake_case
                return sortField.contains("_") ? sortField : convertCamelToSnake(sortField);
        }
    }
    
    private String convertCamelToSnake(String camelCase) {
        // Simple conversion: add underscore before uppercase letters
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody ProductUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductResponseDTO>> getAllActiveProducts() {
        List<ProductResponseDTO> products = service.getAllActiveProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductSimpleDTO> getProductBySku(@PathVariable String sku) {
        ProductSimpleDTO product = service.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(@PathVariable Long categoryId) {
        List<ProductResponseDTO> products = service.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}/paginated")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsByCategoryPaginated(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<ProductResponseDTO> products = service.getProductsByCategoryPaginated(categoryId, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search/quick")
    public ResponseEntity<List<ProductSimpleDTO>> quickSearch(@RequestParam String q) {
        List<ProductSimpleDTO> products = service.quickSearch(q);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/stats")
    public ResponseEntity<ProductStatsDTO> getProductStats(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sku) {
        return ResponseEntity.ok(service.getProductStats(brand, isActive, minPrice, maxPrice, sku));
    }
}