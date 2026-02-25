package com.example.back_end.modules.catalog.product.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.back_end.modules.catalog.product.dto.*;
import com.example.back_end.modules.catalog.product.entity.Media;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.catalog.product.entity.ProductMediaId;
import com.example.back_end.modules.catalog.product.mapper.ProductMapper;
import com.example.back_end.modules.catalog.product.repository.MediaRepository;
import com.example.back_end.modules.catalog.product.repository.ProductMediaRepository;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.store_product.entity.StockSnapshot;
import com.example.back_end.modules.store_product.repository.StockSnapshotRepository;
import com.example.back_end.modules.store_product.repository.InventoryMovementRepository;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
import com.example.back_end.modules.catalog.category.service.CategoryService;
import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.stock.entity.InventoryMovement;
import com.example.back_end.modules.stock.enums.InventoryLocationType;
import com.example.back_end.modules.stock.enums.InventoryRefType;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final StockSnapshotRepository stockSnapshotRepository;
    private final MediaRepository mediaRepository;
    private final ProductMediaRepository productMediaRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ImageStorageService imageStorageService;
    private final InventoryMovementRepository inventoryMovementRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ProductResponseDTO create(ProductCreateDTO dto) {
        // Validate SKU if provided
        if (dto.getSku() != null) {
            String sku = dto.getSku().trim();
            if (sku.isEmpty()) {
                throw new IllegalArgumentException("SKU cannot be blank");
            }
            if (repository.existsBySku(sku)) {
                throw new IllegalArgumentException("SKU already exists: " + sku);
            }
        }
        if (dto.getParentCategoryId() == null) {
            throw new IllegalArgumentException("Parent category is required");
        }

        Product saved = repository.save(ProductMapper.toEntity(dto));
        saved.getProductMedia().size(); // Force load productMedia

        // Attach parent and subcategory
        Set<Category> categories = resolveCategoriesForProduct(dto.getParentCategoryId(), dto.getSubCategoryId());
        saved.setCategories(categories);
        repository.save(saved);
        saved.getCategories().size(); // Force load categories

        // Get stock snapshot (if exists) - will be null for new products
        StockSnapshot snapshot = stockSnapshotRepository.findById(saved.getId()).orElse(null);

        return ProductMapper.toResponse(saved, snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getById(Long id) {
        // Default behavior: don't include stock and categories (defaults to false)
        return getById(id, false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getById(Long id, boolean includeStock, boolean includeCategories) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        // Force load productMedia (always needed)
        p.getProductMedia().size();
        
        // Create DTO - use a version that doesn't auto-load categories
        ProductResponseDTO dto = ProductMapper.toResponse(p);
        
        // Clear categories initially (we'll set them conditionally)
        dto.setCategories(null);
        
        // Include stock quantities if requested
        if (includeStock) {
            Optional<StockSnapshot> stockOpt = stockSnapshotRepository.findById(id);
            if (stockOpt.isPresent()) {
                StockSnapshot snapshot = stockOpt.get();
                dto.setWarehouseQty(snapshot.getWarehouseQty() != null ? snapshot.getWarehouseQty() : BigDecimal.ZERO);
                dto.setStoreQty(snapshot.getStoreQty() != null ? snapshot.getStoreQty() : BigDecimal.ZERO);
            } else {
                // Product not in stock table, set to 0
                dto.setWarehouseQty(BigDecimal.ZERO);
                dto.setStoreQty(BigDecimal.ZERO);
            }
        } else {
            // Set to null if not requested
            dto.setWarehouseQty(null);
            dto.setStoreQty(null);
        }
        
        // Include categories if requested
        if (includeCategories) {
            List<CategoryDTO> categories = categoryService.getByProductId(id);
            // Ensure we return an empty array instead of null when no categories
            dto.setCategories(categories != null ? categories : new java.util.ArrayList<>());
        } else {
            // Explicitly set to null when not requested
            dto.setCategories(null);
        }
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> search(String q, Pageable pageable) {
        // Normalize empty string to null
        String normalizedQ = (q != null && q.trim().isEmpty()) ? null : q;
        return repository.search(normalizedQ, pageable)
                .map(product -> {
                    // Force load productMedia
                    product.getProductMedia().size();
                    product.getCategories().size();
                    // Get stock snapshot (if exists)
                    StockSnapshot snapshot = stockSnapshotRepository.findById(product.getId()).orElse(null);
                    return ProductMapper.toResponse(product, snapshot);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> filter(String brand, Boolean isActive,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           String sku, Integer minWarehouseQuantity,
                                           Integer minStoreQuantity, String search,
                                           Boolean lowStock, Boolean outOfStock,
                                           Pageable pageable,
                                           boolean includeStock, boolean includeCategories) {
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        
        // ✅ STEP 1: Apply filters and pagination
        // Handle special sorting cases (warehouseQuantity, sales) that require joins
        Page<Product> products;
        Sort.Order sortOrder = pageable.getSort().isSorted() ? 
                pageable.getSort().iterator().next() : null;
        
        if (sortOrder != null && ("warehouseQty".equals(sortOrder.getProperty()) || 
                                  "warehouseQty_with_zero_last".equals(sortOrder.getProperty()) ||
                                  "sales".equals(sortOrder.getProperty()))) {
            // For warehouseQuantity and sales sorting, we need to fetch all matching products,
            // sort them, then paginate (less efficient but necessary for these fields)
            List<Product> allProducts = repository.filterList(brand, isActive, minPrice, maxPrice, sku,
                    minWarehouseQuantity, minStoreQuantity, search, lowStock, outOfStock);
            
            // Sort in memory
            if ("warehouseQty".equals(sortOrder.getProperty())) {
                allProducts.sort((p1, p2) -> {
                    BigDecimal qty1 = getWarehouseQuantity(p1.getId());
                    BigDecimal qty2 = getWarehouseQuantity(p2.getId());
                    int comparison = qty1.compareTo(qty2);
                    return sortOrder.getDirection() == Sort.Direction.ASC ? comparison : -comparison;
                });
            } else if ("warehouseQty_with_zero_last".equals(sortOrder.getProperty())) {
                // Sort by warehouseQuantity DESC, but put zero quantities at the bottom
                allProducts.sort((p1, p2) -> {
                    BigDecimal qty1 = getWarehouseQuantity(p1.getId());
                    BigDecimal qty2 = getWarehouseQuantity(p2.getId());
                    boolean isZero1 = qty1.compareTo(BigDecimal.ZERO) == 0;
                    boolean isZero2 = qty2.compareTo(BigDecimal.ZERO) == 0;
                    
                    // If one is zero and the other is not, zero goes to bottom
                    if (isZero1 && !isZero2) return 1;  // p1 (zero) goes after p2
                    if (!isZero1 && isZero2) return -1; // p2 (zero) goes after p1
                    
                    // Both are zero or both are non-zero: sort by quantity DESC
                    int comparison = qty2.compareTo(qty1); // DESC order (qty2.compareTo(qty1))
                    return comparison;
                });
            } else if ("sales".equals(sortOrder.getProperty())) {
                allProducts.sort((p1, p2) -> {
                    BigDecimal sales1 = getSalesCount(p1.getId());
                    BigDecimal sales2 = getSalesCount(p2.getId());
                    int comparison = sales1.compareTo(sales2);
                    return sortOrder.getDirection() == Sort.Direction.ASC ? comparison : -comparison;
                });
            }
            
            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allProducts.size());
            List<Product> paginatedProducts = start < allProducts.size() ? 
                    allProducts.subList(start, end) : List.of();
            
            products = new PageImpl<>(
                    paginatedProducts, pageable, allProducts.size());
        } else {
            // Use standard pagination for other sort fields
            // For standard sorting, we can use the Pageable directly
            products = repository.filter(brand, isActive, minPrice, maxPrice, sku,
                    minWarehouseQuantity, minStoreQuantity, search, lowStock, outOfStock, pageable);
        }
        
        // ✅ STEP 2: Map only the paginated products (e.g., 10 products, not all)
        // Ensure we always return a proper Page structure, even when empty
        Page<ProductResponseDTO> productDTOs = products.map(product -> {
            // Force load productMedia (always needed)
            product.getProductMedia().size();
            
            // Create DTO - use a version that doesn't auto-load categories
            ProductResponseDTO dto = ProductMapper.toResponse(product);
            
            // Clear categories initially (we'll set them conditionally)
            dto.setCategories(null);
            
            // ✅ STEP 3: Fetch stock ONLY for the paginated products (e.g., 10 products)
            if (includeStock) {
                Optional<StockSnapshot> stockOpt = stockSnapshotRepository.findById(product.getId());
                if (stockOpt.isPresent()) {
                    StockSnapshot snapshot = stockOpt.get();
                    dto.setWarehouseQty(snapshot.getWarehouseQty() != null ? snapshot.getWarehouseQty() : BigDecimal.ZERO);
                    dto.setStoreQty(snapshot.getStoreQty() != null ? snapshot.getStoreQty() : BigDecimal.ZERO);
                } else {
                    // Product not in stock table, set to 0
                    dto.setWarehouseQty(BigDecimal.ZERO);
                    dto.setStoreQty(BigDecimal.ZERO);
                }
            } else {
                // Set to null if not requested
                dto.setWarehouseQty(null);
                dto.setStoreQty(null);
            }
            
            // ✅ STEP 4: Fetch categories ONLY for the paginated products (e.g., 10 products)
            if (includeCategories) {
                List<CategoryDTO> categories = categoryService.getByProductId(product.getId());
                // Ensure we return an empty array instead of null when no categories
                dto.setCategories(categories != null ? categories : new java.util.ArrayList<>());
            } else {
                // Explicitly set to null when not requested
                dto.setCategories(null);
            }
            
            return dto;
        });
        
        // Spring's Page.map() preserves the pagination structure, so we always get a proper Page response
        // even when content is empty. The response will include:
        // - content: [] (empty array when no results)
        // - totalElements: 0
        // - totalPages: 0
        // - number: current page number
        // - size: page size
        // - first, last, etc.
        return productDTOs;
    }

    @Override
    public ProductResponseDTO update(Long id, ProductUpdateDTO dto) {
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        // Validate SKU if provided
        if (dto.getSku() != null) {
            String sku = dto.getSku().trim();
            if (sku.isEmpty()) {
                throw new IllegalArgumentException("SKU cannot be blank");
            }
            if (!sku.equals(p.getSku()) && repository.existsBySku(sku)) {
                throw new IllegalArgumentException("SKU already exists: " + sku);
            }
        }
        ProductMapper.updateEntity(dto, p);
        Product saved = repository.save(p);
        saved.getProductMedia().size(); // Force load productMedia

        // Update categories if provided
        if (dto.getParentCategoryId() != null) {
            Set<Category> categories = resolveCategoriesForProduct(dto.getParentCategoryId(), dto.getSubCategoryId());
            saved.setCategories(categories);
            saved = repository.save(saved);
        }
        saved.getCategories().size(); // Force load categories

        // Get stock snapshot (if exists)
        StockSnapshot snapshot = stockSnapshotRepository.findById(id).orElse(null);

        return ProductMapper.toResponse(saved, snapshot);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // Fetch product with all relationships loaded
        Product product = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        
        // Force load relationships to avoid lazy loading issues
        product.getCategories().size();
        product.getProductMedia().size();

        // Merge product to ensure it's managed by EntityManager BEFORE creating related entities
        Product managedProduct = entityManager.merge(product);

        // Get stock snapshot before deletion
        StockSnapshot snapshot = stockSnapshotRepository.findById(id).orElse(null);
        
        BigDecimal unitCost = managedProduct.getDefaultCost() != null ? managedProduct.getDefaultCost() : BigDecimal.ZERO;
        BigDecimal warehouseQty = BigDecimal.ZERO;
        BigDecimal storeQty = BigDecimal.ZERO;
        
        // Get quantities from snapshot if it exists
        if (snapshot != null) {
            warehouseQty = snapshot.getWarehouseQty() != null ? snapshot.getWarehouseQty() : BigDecimal.ZERO;
            storeQty = snapshot.getStoreQty() != null ? snapshot.getStoreQty() : BigDecimal.ZERO;
        } else {
            // If no snapshot, calculate from existing movements
            List<InventoryMovement> existingMovements = inventoryMovementRepository.findByProductId(id);
            if (!existingMovements.isEmpty()) {
                warehouseQty = existingMovements.stream()
                        .filter(m -> m.getLocationType() == InventoryLocationType.WAREHOUSE)
                        .map(InventoryMovement::getQtyChange)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                storeQty = existingMovements.stream()
                        .filter(m -> m.getLocationType() == InventoryLocationType.STORE)
                        .map(InventoryMovement::getQtyChange)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }
        
        // Create movement records for any remaining inventory BEFORE deleting the product
        // Use managedProduct to ensure proper entity management
        if (warehouseQty.compareTo(BigDecimal.ZERO) > 0) {
            InventoryMovement warehouseRemoval = InventoryMovement.builder()
                    .product(managedProduct)
                    .locationType(InventoryLocationType.WAREHOUSE)
                    .refType(InventoryRefType.ADJUSTMENT)
                    .qtyChange(warehouseQty.negate())
                    .unitCost(unitCost)
                    .note("Product deleted - warehouse inventory removed")
                    .build();
            inventoryMovementRepository.saveAndFlush(warehouseRemoval);
        }
        
        if (storeQty.compareTo(BigDecimal.ZERO) > 0) {
            InventoryMovement storeRemoval = InventoryMovement.builder()
                    .product(managedProduct)
                    .locationType(InventoryLocationType.STORE)
                    .refType(InventoryRefType.ADJUSTMENT)
                    .qtyChange(storeQty.negate())
                    .unitCost(unitCost)
                    .note("Product deleted - store inventory removed")
                    .build();
            inventoryMovementRepository.saveAndFlush(storeRemoval);
        }

        // Get all ProductMedia for this product before deletion
        List<ProductMedia> productMediaList = productMediaRepository.findByProductIdOrderBySortOrderAsc(id);

        // Delete physical files from storage (don't let exceptions prevent deletion)
        for (ProductMedia productMedia : productMediaList) {
            if (productMedia.getMedia() != null && productMedia.getMedia().getUrl() != null) {
                String url = productMedia.getMedia().getUrl();
                try {
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    imageStorageService.delete(id, fileName);
                } catch (Exception e) {
                    // Log but don't fail if file deletion fails
                }
            }
        }

        // Clear categories relationship (many-to-many) using managed product
        managedProduct.getCategories().clear();
        entityManager.flush();

        // Delete ProductMedia explicitly (should cascade but being explicit)
        if (!productMediaList.isEmpty()) {
            productMediaRepository.deleteAll(productMediaList);
            productMediaRepository.flush();
        }

        // Delete StockSnapshot if it exists
        if (snapshot != null) {
            stockSnapshotRepository.delete(snapshot);
            stockSnapshotRepository.flush();
        }

        // Delete InventoryMovement records explicitly (they reference the product)
        // Note: We created deletion records above, but they'll be cascade deleted with the product
        // So we delete all movements first to avoid foreign key constraint issues
        List<InventoryMovement> allMovements = inventoryMovementRepository.findByProductId(id);
        if (!allMovements.isEmpty()) {
            // Delete movements one by one to avoid batch issues
            for (InventoryMovement movement : allMovements) {
                inventoryMovementRepository.delete(movement);
            }
            inventoryMovementRepository.flush();
        }
        
        // Now delete the product itself using native SQL to bypass any JPA issues
        int deletedCount = entityManager.createNativeQuery("DELETE FROM products WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
        
        entityManager.flush();
        
        if (deletedCount == 0) {
            throw new IllegalStateException("Failed to delete product with id: " + id + " - no rows were deleted");
        }
        
        // Clear persistence context to ensure changes are visible
        entityManager.clear();

        // Delete orphaned Media records (no longer referenced by any ProductMedia)
        List<Media> orphanedMedia = mediaRepository.findOrphanedMedia();
        if (!orphanedMedia.isEmpty()) {
            mediaRepository.deleteAll(orphanedMedia);
        }
    }

    @Override
    public ProductResponseDTO addImage(Long productId, AddProductMediaDTO dto) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be blank");
        }

        Media media = Media.builder()
                .url(dto.getUrl().trim())
                .mimeType(dto.getMimeType() != null ? dto.getMimeType().trim() : null)
                .title(dto.getTitle() != null ? dto.getTitle().trim() : null)
                .altText(dto.getAltText() != null ? dto.getAltText().trim() : null)
                .build();
        media = mediaRepository.save(media);

        if (dto.getIsPrimary() != null && dto.getIsPrimary()) {
            productMediaRepository.clearPrimaryFlags(productId);
        }

        ProductMedia productMedia = ProductMedia.builder()
                .id(new ProductMediaId(productId, media.getId()))
                .product(product)
                .media(media)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .build();

        productMediaRepository.save(productMedia);

        repository.flush();
        Product updatedProduct = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        updatedProduct.getProductMedia().size();
        updatedProduct.getCategories().size();

        StockSnapshot snapshot = stockSnapshotRepository.findById(productId).orElse(null);
        return ProductMapper.toResponse(updatedProduct, snapshot);
    }

    @Override
    public ProductResponseDTO updateImage(Long productId, Long mediaId, UpdateProductMediaDTO dto) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        ProductMedia productMedia = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found for this product"));

        if (productMedia.getMedia() == null) {
            throw new IllegalStateException("Media entity is null for this product media");
        }

        if (dto.getUrl() != null && dto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be blank");
        }

        // Update Media fields
        Media media = productMedia.getMedia();
        if (dto.getUrl() != null) media.setUrl(dto.getUrl().trim());
        if (dto.getMimeType() != null) media.setMimeType(dto.getMimeType().trim());
        if (dto.getTitle() != null) media.setTitle(dto.getTitle().trim());
        if (dto.getAltText() != null) media.setAltText(dto.getAltText().trim());
        mediaRepository.save(media);

        // Update ProductMedia fields
        if (dto.getSortOrder() != null) {
            productMedia.setSortOrder(dto.getSortOrder());
        }
        if (dto.getIsPrimary() != null) {
            if (dto.getIsPrimary() && !productMedia.getIsPrimary()) {
                productMediaRepository.clearPrimaryFlags(productId);
            }
            productMedia.setIsPrimary(dto.getIsPrimary());
        }

        productMediaRepository.save(productMedia);

        Product updatedProduct = repository.findById(productId).orElse(product);
        updatedProduct.getProductMedia().size();
        updatedProduct.getCategories().size();

        StockSnapshot snapshot = stockSnapshotRepository.findById(productId).orElse(null);
        return ProductMapper.toResponse(updatedProduct, snapshot);
    }

    @Override
    public void removeImage(Long productId, Long mediaId) {
        if (!repository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        ProductMedia productMedia = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found for this product"));

        Long mediaIdToCheck = productMedia.getMedia() != null ? productMedia.getMedia().getId() : null;

        if (productMedia.getMedia() != null && productMedia.getMedia().getUrl() != null) {
            String url = productMedia.getMedia().getUrl();
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            try {
                imageStorageService.delete(productId, fileName);
            } catch (Exception e) {
                // Log but don't fail
            }
        }

        productMediaRepository.delete(productMedia);

        if (mediaIdToCheck != null) {
            List<Media> orphanedMedia = mediaRepository.findOrphanedMedia();
            orphanedMedia.stream()
                    .filter(m -> m.getId().equals(mediaIdToCheck))
                    .findFirst()
                    .ifPresent(mediaRepository::delete);
        }
    }

    @Override
    public ProductResponseDTO setPrimaryImage(Long productId, Long mediaId) {
        if (!repository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        ProductMedia productMedia = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found for this product"));

        productMediaRepository.clearPrimaryFlags(productId);
        productMedia.setIsPrimary(true);
        productMediaRepository.save(productMedia);

        repository.flush();
        Product updatedProduct = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        updatedProduct.getProductMedia().size();
        updatedProduct.getCategories().size();

        StockSnapshot snapshot = stockSnapshotRepository.findById(productId).orElse(null);
        return ProductMapper.toResponse(updatedProduct, snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllActiveProducts() {
        List<Product> products = repository.findAllActiveProducts();
        return products.stream()
                .map(product -> {
                    product.getProductMedia().size();
                    product.getCategories().size();
                    StockSnapshot snapshot = stockSnapshotRepository.findById(product.getId()).orElse(null);
                    return ProductMapper.toResponse(product, snapshot);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSimpleDTO getProductBySku(String sku) {
        Product product = repository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with SKU: " + sku));

        if (!product.getIsActive()) {
            throw new IllegalStateException("Product is inactive: " + sku);
        }

        return ProductMapper.toSimpleDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId) {
        List<Product> products = repository.findProductsByCategoryId(categoryId);
        return products.stream()
                .map(product -> {
                    product.getProductMedia().size();
                    product.getCategories().size();
                    StockSnapshot snapshot = stockSnapshotRepository.findById(product.getId()).orElse(null);
                    return ProductMapper.toResponse(product, snapshot);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> getProductsByCategoryPaginated(Long categoryId, Pageable pageable) {
        Page<Product> products = repository.findProductsByCategoryIdPaginated(categoryId, pageable);
        return products.map(product -> {
            product.getProductMedia().size();
            product.getCategories().size();
            StockSnapshot snapshot = stockSnapshotRepository.findById(product.getId()).orElse(null);
            return ProductMapper.toResponse(product, snapshot);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSimpleDTO> quickSearch(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return List.of();
        }

        List<Product> products = repository.quickSearch(searchTerm);
        return products.stream()
                .map(ProductMapper::toSimpleDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatsDTO getProductStats(String brand, Boolean isActive, BigDecimal minPrice, BigDecimal maxPrice, String sku) {
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }

        // Count total products
        long totalProducts = repository.countWithFilters(brand, isActive, minPrice, maxPrice, sku);

        // Count active products
        long activeProducts = repository.countActiveWithFilters(brand, minPrice, maxPrice, sku);

        // Count inactive products
        long inactiveProducts = totalProducts - activeProducts;

        // Count low stock products (threshold: 10)
        BigDecimal lowStockThreshold = BigDecimal.valueOf(10);
        long lowStock = repository.countLowStock(brand, isActive, minPrice, maxPrice, sku, lowStockThreshold);

        // Count out of stock products
        long outOfStock = repository.countOutOfStock(brand, isActive, minPrice, maxPrice, sku);

        // Count in stock products
        long inStock = totalProducts - outOfStock;

        // Calculate total inventory value
        BigDecimal totalValue = repository.calculateTotalValue(brand, isActive, minPrice, maxPrice, sku);

        return ProductStatsDTO.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .lowStock(lowStock)
                .outOfStock(outOfStock)
                .inStock(inStock)
                .totalValue(totalValue)
                .build();
    }

    private BigDecimal getWarehouseQuantity(Long productId) {
        Optional<StockSnapshot> snapshot = stockSnapshotRepository.findById(productId);
        return snapshot.map(s -> s.getWarehouseQty() != null ? s.getWarehouseQty() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal getSalesCount(Long productId) {
        // Query order_items to get total quantity sold for this product from PAID orders
        // Using a simple native query approach
        String sql = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.product_id = :productId
              AND o.status = 'PAID'
            """;
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("productId", productId)
                .getSingleResult();
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }
    
    private Set<Category> resolveCategoriesForProduct(Long parentCategoryId, Long subCategoryId) {
        Set<Category> categories = new HashSet<>();

        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found: " + parentCategoryId));
        categories.add(parent);

        if (subCategoryId != null) {
            Category sub = categoryRepository.findById(subCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Subcategory not found: " + subCategoryId));
            categories.add(sub);
        }

        return categories;
    }
}

