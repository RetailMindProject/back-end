package com.example.back_end.modules.offer.service;

import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
import com.example.back_end.modules.offer.dto.BundleItemDTO;
import com.example.back_end.modules.offer.dto.OfferRequestDTO;
import com.example.back_end.modules.offer.dto.OfferResponseDTO;
import com.example.back_end.modules.offer.entity.*;
import com.example.back_end.modules.offer.mapper.OfferMapper;
import com.example.back_end.modules.offer.repository.*;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final OfferProductRepository offerProductRepository;
    private final OfferCategoryRepository offerCategoryRepository;
    private final OfferBundleRepository offerBundleRepository;
    private final OrderOfferRepository orderOfferRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OfferMapper offerMapper;
    private final EntityManager entityManager;

    @Override
    public OfferResponseDTO createOffer(OfferRequestDTO requestDTO) {
        log.info("Creating new offer with type: {}", requestDTO.getOfferType());

        // Validate unique code if provided
        if (requestDTO.getCode() != null && offerRepository.existsByCode(requestDTO.getCode())) {
            throw new IllegalArgumentException("Offer code already exists: " + requestDTO.getCode());
        }

        // Create base offer entity
        Offer offer = Offer.builder()
                .code(requestDTO.getCode())
                .title(requestDTO.getTitle())
                .description(requestDTO.getDescription())
                .offerType(requestDTO.getOfferType())
                .discountType(requestDTO.getDiscountType())
                .discountValue(requestDTO.getDiscountValue())
                .startAt(requestDTO.getStartAt())
                .endAt(requestDTO.getEndAt())
                .isActive(requestDTO.getIsActive())
                .build();

        // Save offer first to get ID
        offer = offerRepository.save(offer);

        // Handle specific offer type details
        switch (requestDTO.getOfferType()) {
            case PRODUCT:
                handleProductOffer(offer, requestDTO.getProductIds());
                break;
            case CATEGORY:
                handleCategoryOffer(offer, requestDTO.getCategoryIds());
                break;
            case ORDER:
                handleOrderOffer(offer, requestDTO);
                break;
            case BUNDLE:
                handleBundleOffer(offer, requestDTO.getBundleItems());
                break;
        }

        // Flush to ensure all changes are persisted before fetching
        entityManager.flush();
        // Clear persistence context to force fresh query from database
        entityManager.clear();

        // Fetch complete offer with relationships
        offer = fetchOfferWithRelationships(offer.getId(), requestDTO.getOfferType());

        log.info("Successfully created offer with ID: {}", offer.getId());
        log.info("Offer products count after fetch: {}", offer.getOfferProducts().size());
        
        return offerMapper.toResponseDTO(offer);
    }

    @Override
    public OfferResponseDTO updateOffer(Long id, OfferRequestDTO requestDTO) {
        log.info("Updating offer with ID: {}", id);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found with ID: " + id));

        // Validate code uniqueness if changed
        if (requestDTO.getCode() != null && !requestDTO.getCode().equals(offer.getCode())) {
            if (offerRepository.existsByCode(requestDTO.getCode())) {
                throw new IllegalArgumentException("Offer code already exists: " + requestDTO.getCode());
            }
        }

        // Update base fields
        offer.setCode(requestDTO.getCode());
        offer.setTitle(requestDTO.getTitle());
        offer.setDescription(requestDTO.getDescription());
        offer.setDiscountType(requestDTO.getDiscountType());
        offer.setDiscountValue(requestDTO.getDiscountValue());
        offer.setStartAt(requestDTO.getStartAt());
        offer.setEndAt(requestDTO.getEndAt());
        offer.setIsActive(requestDTO.getIsActive());

        // Clear and update relationships based on offer type
        clearOfferRelationships(offer);

        switch (requestDTO.getOfferType()) {
            case PRODUCT:
                handleProductOffer(offer, requestDTO.getProductIds());
                break;
            case CATEGORY:
                handleCategoryOffer(offer, requestDTO.getCategoryIds());
                break;
            case ORDER:
                handleOrderOffer(offer, requestDTO);
                break;
            case BUNDLE:
                handleBundleOffer(offer, requestDTO.getBundleItems());
                break;
        }

        offer = offerRepository.save(offer);
        offer = fetchOfferWithRelationships(offer.getId(), requestDTO.getOfferType());

        log.info("Successfully updated offer with ID: {}", id);
        return offerMapper.toResponseDTO(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferResponseDTO getOfferById(Long id) {
        log.info("=== START getOfferById ===");

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found with ID: " + id));

        log.info("Offer type: {}", offer.getOfferType());
        log.info("Before fetch - offerProducts size: {}", offer.getOfferProducts().size());

        offer = fetchOfferWithRelationships(id, offer.getOfferType());

        log.info("After fetch - offerProducts size: {}", offer.getOfferProducts().size());

        if (!offer.getOfferProducts().isEmpty()) {
            OfferProduct firstOp = offer.getOfferProducts().iterator().next();
            log.info("First OfferProduct: {}", firstOp);
            log.info("First Product: {}", firstOp.getProduct());
            log.info("First Product Name: {}", firstOp.getProduct().getName());
        }

        log.info("=== END getOfferById ===");
        return offerMapper.toResponseDTO(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferResponseDTO> getAllOffers(Pageable pageable) {
        log.info("Fetching all offers with pagination");
        Page<Offer> offers = offerRepository.findAll(pageable);
        return offers.map(offerMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfferResponseDTO> getActiveOffers() {
        log.info("Fetching all active offers");
        List<Offer> offers = offerRepository.findActiveOffers(LocalDateTime.now());
        return offerMapper.toResponseDTOList(offers);
    }

    @Override
    public void deleteOffer(Long id) {
        log.info("Deleting offer with ID: {}", id);

        if (!offerRepository.existsById(id)) {
            throw new IllegalArgumentException("Offer not found with ID: " + id);
        }

        offerRepository.deleteById(id);
        log.info("Successfully deleted offer with ID: {}", id);
    }

    @Override
    public OfferResponseDTO toggleOfferStatus(Long id) {
        log.info("Toggling status for offer with ID: {}", id);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found with ID: " + id));

        offer.setIsActive(!offer.getIsActive());
        offer = offerRepository.save(offer);

        log.info("Successfully toggled offer status to: {}", offer.getIsActive());
        return offerMapper.toResponseDTO(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean offerCodeExists(String code) {
        return offerRepository.existsByCode(code);
    }

    // Private helper methods

    private void handleProductOffer(Offer offer, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs are required for PRODUCT offer");
        }

        for (Long productId : productIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            // Check if OfferProduct already exists
            OfferProduct.OfferProductId compositeId = new OfferProduct.OfferProductId(offer.getId(), product.getId());
            
            // Only create and save if it doesn't exist
            if (!offerProductRepository.existsById(compositeId)) {
                OfferProduct offerProduct = new OfferProduct();
                offerProduct.setId(compositeId);
                offerProduct.setOffer(offer);
                offerProduct.setProduct(product);

                // Save the offerProduct to database
                offerProductRepository.save(offerProduct);
                log.info("Saved OfferProduct: offerId={}, productId={}", offer.getId(), productId);
            } else {
                log.warn("OfferProduct already exists: offerId={}, productId={}", offer.getId(), productId);
            }
        }
    }

    private void handleCategoryOffer(Offer offer, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Category IDs are required for CATEGORY offer");
        }

        for (Long categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

            OfferCategory offerCategory = new OfferCategory();
            offerCategory.setId(new OfferCategory.OfferCategoryId(offer.getId(), category.getId()));
            offerCategory.setOffer(offer);
            offerCategory.setCategory(category);

            offerCategoryRepository.save(offerCategory);
        }
    }

    private void handleOrderOffer(Offer offer, OfferRequestDTO requestDTO) {
        if (requestDTO.getMinOrderAmount() == null) {
            throw new IllegalArgumentException("Minimum order amount is required for ORDER offer");
        }

        OrderOffer orderOffer = OrderOffer.builder()
                .offer(offer)
                .minOrderAmount(requestDTO.getMinOrderAmount())
                .applyOnce(requestDTO.getApplyOnce() != null ? requestDTO.getApplyOnce() : true)
                .build();

        orderOfferRepository.save(orderOffer);
    }

    private void handleBundleOffer(Offer offer, List<BundleItemDTO> bundleItems) {
        if (bundleItems == null || bundleItems.size() < 2) {
            throw new IllegalArgumentException("Bundle offer requires at least 2 products");
        }

        for (BundleItemDTO item : bundleItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + item.getProductId()));

            OfferBundle offerBundle = OfferBundle.builder()
                    .offer(offer)
                    .product(product)
                    .requiredQty(item.getRequiredQty())
                    .build();

            offerBundleRepository.save(offerBundle);
        }
    }

    private void clearOfferRelationships(Offer offer) {
        offerProductRepository.deleteByOfferId(offer.getId());
        offerCategoryRepository.deleteByOfferId(offer.getId());
        offerBundleRepository.deleteByOfferId(offer.getId());
        orderOfferRepository.deleteByOfferId(offer.getId());
    }

    private Offer fetchOfferWithRelationships(Long id, Offer.OfferType offerType) {
        switch (offerType) {
            case PRODUCT:
                Offer offer = offerRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
                // Manually load offerProducts to ensure they are loaded
                List<OfferProduct> offerProducts = offerProductRepository.findByOfferId(id);
                if (offerProducts != null && !offerProducts.isEmpty()) {
                    offer.getOfferProducts().clear();
                    offer.getOfferProducts().addAll(offerProducts);
                    log.info("Loaded {} offerProducts for offer {}", offerProducts.size(), id);
                }
                return offer;
            case CATEGORY:
                return offerRepository.findByIdWithCategories(id)
                        .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
            case ORDER:
                return offerRepository.findByIdWithOrderOffer(id)
                        .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
            case BUNDLE:
                return offerRepository.findByIdWithBundles(id)
                        .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
            default:
                return offerRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
        }
    }

}