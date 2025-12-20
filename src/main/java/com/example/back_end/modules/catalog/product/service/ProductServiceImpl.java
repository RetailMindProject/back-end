package com.example.back_end.modules.catalog.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
import com.example.back_end.modules.catalog.product.service.ImageStorageService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
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
    private final ImageStorageService imageStorageService;

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
        Product p = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        // Force load productMedia
        p.getProductMedia().size();
        p.getCategories().size();

        // Get stock snapshot (if exists)
        StockSnapshot snapshot = stockSnapshotRepository.findById(id).orElse(null);

        return ProductMapper.toResponse(p, snapshot);
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
                                           String sku, Pageable pageable) {
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        return repository.filter(brand, isActive, minPrice, maxPrice, sku, pageable)
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
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }

        // Get all ProductMedia for this product before deletion
        List<ProductMedia> productMediaList = productMediaRepository.findByProductIdOrderBySortOrderAsc(id);

        // Delete physical files from storage
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

        // Delete the product (cascade delete ProductMedia)
        repository.deleteById(id);

        // Delete orphaned Media records
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

