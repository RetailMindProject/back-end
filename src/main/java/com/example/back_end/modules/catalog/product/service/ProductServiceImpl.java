package com.example.back_end.modules.catalog.product.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.back_end.modules.catalog.product.dto.AddProductMediaDTO;
import com.example.back_end.modules.catalog.product.dto.ProductCreateDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.ProductUpdateDTO;
import com.example.back_end.modules.catalog.product.dto.UpdateProductMediaDTO;
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

    @Override @Transactional(readOnly = true)
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

    @Override @Transactional(readOnly = true)
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

    @Override @Transactional(readOnly = true)
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
                // URL format: /api/products/{productId}/images/{fileName}
                try {
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    imageStorageService.delete(id, fileName);
                } catch (Exception e) {
                    // Log but don't fail if file deletion fails
                    // The database records will still be deleted
                }
            }
        }
        
        // Delete the product (this will cascade delete ProductMedia due to orphanRemoval = true)
        repository.deleteById(id);
        
        // Find and delete orphaned Media records (Media not referenced by any ProductMedia)
        List<Media> orphanedMedia = mediaRepository.findOrphanedMedia();
        if (!orphanedMedia.isEmpty()) {
            mediaRepository.deleteAll(orphanedMedia);
        }
    }

    @Override
    public ProductResponseDTO addImage(Long productId, AddProductMediaDTO dto) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        // Validate URL is not blank
        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be blank");
        }

        // Always create a new Media entity for each upload (allows duplicates)
        Media media = Media.builder()
                .url(dto.getUrl().trim())
                .mimeType(dto.getMimeType() != null ? dto.getMimeType().trim() : null)
                .title(dto.getTitle() != null ? dto.getTitle().trim() : null)
                .altText(dto.getAltText() != null ? dto.getAltText().trim() : null)
                .build();
        media = mediaRepository.save(media);

        // If this is set as primary, clear other primary flags
        if (dto.getIsPrimary() != null && dto.getIsPrimary()) {
            productMediaRepository.clearPrimaryFlags(productId);
        }

        // Create ProductMedia relationship - always create new (allows duplicates)
        ProductMedia productMedia = ProductMedia.builder()
                .id(new ProductMediaId(productId, media.getId()))
                .product(product)
                .media(media)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .build();

        productMediaRepository.save(productMedia);

        // Reload product with media to avoid session conflicts
        repository.flush();
        Product updatedProduct = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        updatedProduct.getProductMedia().size();
        updatedProduct.getCategories().size();
        return ProductMapper.toResponse(updatedProduct);
    }

    @Override
    public ProductResponseDTO updateImage(Long productId, Long mediaId, UpdateProductMediaDTO dto) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        ProductMedia productMedia = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found for this product"));

        // Validate media exists
        if (productMedia.getMedia() == null) {
            throw new IllegalStateException("Media entity is null for this product media");
        }

        // Validate URL if provided
        if (dto.getUrl() != null && dto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be blank");
        }

        // If URL is being changed
        if (dto.getUrl() != null && !dto.getUrl().trim().equals(productMedia.getMedia().getUrl())) {
            // Check if new URL already exists as a Media
            Media existingMedia = mediaRepository.findByUrl(dto.getUrl()).orElse(null);
            
            if (existingMedia != null && !existingMedia.getId().equals(mediaId)) {
                // New URL exists as different Media - need to switch ProductMedia to use it
                // First delete old ProductMedia
                productMediaRepository.delete(productMedia);
                
                // Create new ProductMedia with existing Media
                ProductMedia newProductMedia = ProductMedia.builder()
                        .id(new ProductMediaId(productId, existingMedia.getId()))
                        .product(product)
                        .media(existingMedia)
                        .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : productMedia.getSortOrder())
                        .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : productMedia.getIsPrimary())
                        .build();
                
                // If setting as primary, clear other primary flags
                if (newProductMedia.getIsPrimary()) {
                    productMediaRepository.clearPrimaryFlags(productId);
                }
                
                productMediaRepository.save(newProductMedia);
                productMedia = newProductMedia;
            } else {
                // Update current Media entity
                Media media = productMedia.getMedia();
                media.setUrl(dto.getUrl().trim());
                if (dto.getMimeType() != null) media.setMimeType(dto.getMimeType().trim());
                if (dto.getTitle() != null) media.setTitle(dto.getTitle().trim());
                if (dto.getAltText() != null) media.setAltText(dto.getAltText().trim());
                mediaRepository.save(media);
            }
        } else {
            // URL not changed, just update Media fields if provided
            Media media = productMedia.getMedia();
            if (dto.getMimeType() != null) media.setMimeType(dto.getMimeType().trim());
            if (dto.getTitle() != null) media.setTitle(dto.getTitle().trim());
            if (dto.getAltText() != null) media.setAltText(dto.getAltText().trim());
            mediaRepository.save(media);
        }

        // Update ProductMedia relationship (sortOrder, isPrimary)
        if (dto.getSortOrder() != null) {
            productMedia.setSortOrder(dto.getSortOrder());
        }
        if (dto.getIsPrimary() != null) {
            if (dto.getIsPrimary() && !productMedia.getIsPrimary()) {
                // Clear other primary flags
                productMediaRepository.clearPrimaryFlags(productId);
            }
            productMedia.setIsPrimary(dto.getIsPrimary());
        }

        productMediaRepository.save(productMedia);

        // Reload product with media
        Product updatedProduct = repository.findById(productId).orElse(product);
        updatedProduct.getProductMedia().size();
        return ProductMapper.toResponse(updatedProduct);
    }

    @Override
    public void removeImage(Long productId, Long mediaId) {
        if (!repository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }

        ProductMedia productMedia = productMediaRepository.findByProductIdAndMediaId(productId, mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found for this product"));

        // Get mediaId before deletion
        Long mediaIdToCheck = productMedia.getMedia() != null ? productMedia.getMedia().getId() : null;
        
        // Extract filename from URL and delete physical file
        if (productMedia.getMedia() != null && productMedia.getMedia().getUrl() != null) {
            String url = productMedia.getMedia().getUrl();
            // URL format: /api/products/{productId}/images/{fileName}
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            try {
                imageStorageService.delete(productId, fileName);
            } catch (Exception e) {
                // Log but don't fail if file deletion fails
                // The database record will still be deleted
            }
        }

        productMediaRepository.delete(productMedia);
        
        // Check if this Media is now orphaned (not used by any other ProductMedia)
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

        // Clear all primary flags for this product
        productMediaRepository.clearPrimaryFlags(productId);

        // Set this image as primary
        productMedia.setIsPrimary(true);
        productMediaRepository.save(productMedia);

        // Reload product with media
        repository.flush();
        Product updatedProduct = repository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        updatedProduct.getProductMedia().size();
        updatedProduct.getCategories().size();
        
        // Get stock snapshot
        StockSnapshot snapshot = stockSnapshotRepository.findById(productId).orElse(null);
        
        return ProductMapper.toResponse(updatedProduct, snapshot);
    }

    private Set<Category> resolveCategoriesForProduct(Long parentCategoryId, Long subCategoryId) {
        Set<Category> categories = new HashSet<>();
        
        // Add parent category (any category can be used as parent)
        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found: " + parentCategoryId));
        categories.add(parent);
        
        // Add subcategory if provided (any category can be used as subcategory)
        if (subCategoryId != null) {
            Category sub = categoryRepository.findById(subCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Subcategory not found: " + subCategoryId));
            categories.add(sub);
        }
        
        return categories;
    }
}
