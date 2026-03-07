package com.example.back_end.modules.customer.service.customerbrowse;

import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.customer.dto.customerbrowse.*;
import com.example.back_end.modules.customer.repository.customerbrowse.OfferCustomerRepository;
import com.example.back_end.modules.customer.repository.customerbrowse.ProductCustomerRepository;
import com.example.back_end.modules.customer.repository.customerbrowse.ProductMediaCustomerRepository;
import com.example.back_end.modules.offer.entity.Offer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductCustomerService {

    private final ProductCustomerRepository productCustomerRepository;
    private final ProductMediaCustomerRepository productMediaCustomerRepository;
    private final OfferResolverService offerResolverService;
    private final OfferCustomerRepository offerCustomerRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<ProductListItemDTO> getProducts(Long categoryId,
                                                          String q,
                                                          Boolean offersOnly,
                                                          BigDecimal minPrice,
                                                          BigDecimal maxPrice,
                                                          String sortKey,
                                                          int page,
                                                          int size) {

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must be <= maxPrice");
        }

        ProductSortKey resolvedSort = ProductSortKey.fromNullable(sortKey);
        Sort sort = switch (resolvedSort) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "defaultPrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "defaultPrice").and(Sort.by(Sort.Direction.DESC, "id"));
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case RELEVANCE -> Sort.by(Sort.Direction.DESC, "id");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        boolean onlyOffers = offersOnly != null && offersOnly;

        Page<Product> productPage = productCustomerRepository.searchActiveProducts(
                categoryId,
                normalize(q),
                minPrice,
                maxPrice,
                onlyOffers,
                LocalDateTime.now(),
                pageable
        );

        List<Product> products = productPage.getContent();
        List<Long> productIds = products.stream().map(Product::getId).toList();

        Map<Long, String> primaryImageUrlByProductId = loadPrimaryImages(productIds);
        Map<Long, OfferMiniDTO> bestOfferByProductId = offerResolverService.resolveBestOffers(productIds, LocalDateTime.now());

        List<ProductListItemDTO> content = products.stream()
                .map(p -> ProductListItemDTO.builder()
                        .id(p.getId())
                        .sku(p.getSku())
                        .name(p.getName())
                        .brand(p.getBrand())
                        .defaultPrice(p.getDefaultPrice())
                        .taxRate(p.getTaxRate())
                        .primaryImageUrl(primaryImageUrlByProductId.get(p.getId()))
                        .offer(bestOfferByProductId.getOrDefault(p.getId(), null))
                        .build())
                .toList();

        return PageResponseDTO.<ProductListItemDTO>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    // Keep backward compatible signature (used by controller before)
    @Transactional(readOnly = true)
    public PageResponseDTO<ProductListItemDTO> getProducts(Long categoryId, String q, int page, int size) {
        return getProducts(categoryId, q, false, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public ProductDetailsDTO getProductDetails(Long productId) {
        Product p = productCustomerRepository.findActiveById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<CategoryDTO> categories = p.getCategories().stream()
                .map(this::toCategoryDto)
                .sorted(Comparator.comparing(CategoryDTO::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<ProductMediaCustomerRepository.ProductImageRow> imageRows = productMediaCustomerRepository.findImagesForProduct(p.getId());
        List<ProductImageDTO> images = imageRows.stream()
                .map(r -> ProductImageDTO.builder()
                        .id(r.getId())
                        .url(r.getUrl())
                        .mimeType(r.getMimeType())
                        .title(r.getTitle())
                        .altText(r.getAltText())
                        .isPrimary(Boolean.TRUE.equals(r.getIsPrimary()))
                        .sortOrder(r.getSortOrder() == null ? 0 : r.getSortOrder())
                        .build())
                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<OfferCustomerRepository.OfferDetailsRow> offerRows = offerCustomerRepository.findActiveOffersForProduct(p.getId(), now);
        List<OfferDTO> offers = offerRows.stream()
                .map(r -> OfferDTO.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .discountType(Offer.DiscountType.valueOf(r.getDiscountType()))
                        .discountValue(r.getDiscountValue())
                        .offerType(Offer.OfferType.valueOf(r.getOfferType()))
                        .startAt(r.getStartAt())
                        .endAt(r.getEndAt())
                        .build())
                .toList();

        return ProductDetailsDTO.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .brand(p.getBrand())
                .description(p.getDescription())
                .defaultPrice(p.getDefaultPrice())
                .taxRate(p.getTaxRate())
                .unit(p.getUnit())
                .categories(categories)
                .images(images)
                .offers(offers)
                .build();
    }

    private Map<Long, String> loadPrimaryImages(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();
        Map<Long, String> map = new HashMap<>();
        for (ProductMediaCustomerRepository.PrimaryImageRow row : productMediaCustomerRepository.findPrimaryImages(productIds)) {
            map.put(row.getProductId(), row.getUrl());
        }
        return map;
    }

    private CategoryDTO toCategoryDto(Category c) {
        Long parentId = c.getParent() == null ? null : c.getParent().getId();
        return CategoryDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .parentId(parentId)
                .build();
    }

    private static String normalize(String q) {
        if (q == null) return null;
        String t = q.trim();
        return t.isEmpty() ? null : t;
    }
}

