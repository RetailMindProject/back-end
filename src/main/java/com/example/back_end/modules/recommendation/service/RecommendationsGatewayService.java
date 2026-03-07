package com.example.back_end.modules.recommendation.service;

import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.entity.ProductMedia;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.repository.OfferRepository;
import com.example.back_end.modules.recommendation.dto.*;
import com.example.back_end.modules.recommendation.dto.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationsGatewayService {

    private final WebClient recommendationWebClient;
    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;

    public RecommendationsResponseDTO getRecommendations(Long customerId,
                                                         String bearerToken,
                                                         int topK,
                                                         int candidateLimit,
                                                         boolean inStockOnly) {
        try {
            log.debug("Calling recommendation service for customer {} with topK={}", customerId, topK);

            RecommendationServiceResponse serviceResponse = recommendationWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/recommendations/pos/customers/{customerId}")
                            .queryParam("topK", topK)
                            .queryParam("candidateLimit", candidateLimit)
                            .queryParam("inStockOnly", inStockOnly)
                            .build(customerId))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        log.error("Recommendation service returned error status: {}", response.statusCode());
                        return Mono.error(new IllegalStateException("Recommendation service error"));
                    })
                    .bodyToMono(RecommendationServiceResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(throwable -> {
                        log.warn("Error calling recommendation service: {}", throwable.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (serviceResponse == null) {
                log.warn("Recommendation service unavailable or returned null - returning error response for customer {}", customerId);
                return createErrorResponse();
            }

            // DESERIALIZATION VERIFICATION LOG
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  DESERIALIZATION VERIFICATION (RAW JSON → DTO)              ║");
            log.info("╚══════════════════════════════════════════════════════════════╝");
            if (serviceResponse.getRows() != null) {
                int recommendedForYouSize = serviceResponse.getRows().getRecommendedForYou() != null ? serviceResponse.getRows().getRecommendedForYou().size() : 0;
                int popularSize = serviceResponse.getRows().getPopular() != null ? serviceResponse.getRows().getPopular().size() : 0;
                int offersSize = serviceResponse.getRows().getOffers() != null ? serviceResponse.getRows().getOffers().size() : 0;
                log.info("✅ [DESER-1] Deserialized rows.recommendedForYou: {} items", recommendedForYouSize);
                log.info("✅ [DESER-2] Deserialized rows.popular: {} items", popularSize);
                log.info("✅ [DESER-3] Deserialized rows.offers: {} items", offersSize);
            } else {
                log.warn("⚠️ [DESER-1-2-3] Deserialized rows is NULL!");
            }
            if (serviceResponse.getMeta() != null) {
                log.info("✅ [DESER-4] Deserialized meta.isColdStart: {}", serviceResponse.getMeta().getIsColdStart());
                log.info("✅ [DESER-5] Deserialized meta.userSegment: {}", serviceResponse.getMeta().getUserSegment());
                log.info("✅ [DESER-6] Deserialized meta.numRecommendedForYou: {}", serviceResponse.getMeta().getNumRecommendedForYou());
                log.info("✅ [DESER-7] Deserialized meta.topK: {}", serviceResponse.getMeta().getTopK());
            } else {
                log.warn("⚠️ [DESER-4-7] Deserialized meta is NULL!");
            }
            log.info("═══════════════════════════════════════════════════════════════");

            return mapToFrontend(customerId, serviceResponse);
        } catch (Exception ex) {
            log.warn("Failed to fetch recommendations for customer {}: {} - returning error response",
                     customerId, ex.getMessage());
            return createErrorResponse();
        }
    }

    private RecommendationsResponseDTO mapToFrontend(Long customerId, RecommendationServiceResponse serviceResponse) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  MAPPING DIAGNOSIS: Recommendation Service → Frontend       ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("🔍 [SERVICE-MAP-1] CustomerId: {}", customerId);
        log.info("🔍 [SERVICE-MAP-2] Service Response Status: {}", serviceResponse.getStatus());

        RecommendationServiceRows serviceRows = serviceResponse.getRows();
        RecommendationServiceMeta serviceMeta = serviceResponse.getMeta();

        // Log RAW data from recommendation service
        log.info("─────────────────────────────────────────────────────────────");
        log.info("📥 RAW DATA FROM RECOMMENDATION SERVICE:");
        log.info("─────────────────────────────────────────────────────────────");

        if (serviceRows != null) {
            int recommendedForYouCount = serviceRows.getRecommendedForYou() != null ? serviceRows.getRecommendedForYou().size() : 0;
            int popularCount = serviceRows.getPopular() != null ? serviceRows.getPopular().size() : 0;
            int offersCount = serviceRows.getOffers() != null ? serviceRows.getOffers().size() : 0;
            log.info("🔍 [SERVICE-MAP-3] Service Rows.recommendedForYou: {} items", recommendedForYouCount);
            log.info("🔍 [SERVICE-MAP-4] Service Rows.popular: {} items", popularCount);
            log.info("🔍 [SERVICE-MAP-5] Service Rows.offers: {} items", offersCount);
        } else {
            log.warn("⚠️ [SERVICE-MAP-3-4-5] Service Rows is NULL!");
        }

        if (serviceMeta != null) {
            log.info("🔍 [SERVICE-MAP-6] Service Meta.isColdStart: {}", serviceMeta.getIsColdStart());
            log.info("🔍 [SERVICE-MAP-7] Service Meta.numRecommendedForYou: {}", serviceMeta.getNumRecommendedForYou());
            log.info("🔍 [SERVICE-MAP-8] Service Meta.userSegment: {}", serviceMeta.getUserSegment());
            log.info("🔍 [SERVICE-MAP-9] Service Meta.topK: {}", serviceMeta.getTopK());
        } else {
            log.warn("⚠️ [SERVICE-MAP-6-9] Service Meta is NULL!");
        }

        // Build response according to frontend requirements
        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus(serviceResponse.getStatus());

        // Map rows: recommendedForYou → recommendedForYou, popular → popular, offers → offers
        log.info("─────────────────────────────────────────────────────────────");
        log.info("🔄 FIELD NAME MAPPING:");
        log.info("─────────────────────────────────────────────────────────────");

        RecommendationRowsDTO rows = new RecommendationRowsDTO();
        if (serviceRows != null) {
            List<RecommendationItemDTO> recommendedForYouList = mapItems(serviceRows.getRecommendedForYou());
            List<RecommendationItemDTO> popularList = mapItems(serviceRows.getPopular());

            rows.setRecommendedForYou(recommendedForYouList);
            rows.setPopular(popularList);

            log.info("✅ [SERVICE-MAP-10] Mapped: service.rows.recommendedForYou → dto.rows.recommendedForYou ({} items)", recommendedForYouList.size());
            log.info("✅ [SERVICE-MAP-11] Mapped: service.rows.popular → dto.rows.popular ({} items)", popularList.size());

            // Filter and complete offers
            int topK = (serviceMeta != null && serviceMeta.getTopK() != null)
                        ? serviceMeta.getTopK()
                        : 10;
            List<RecommendationItemDTO> offersList = filterAndCompleteOffers(serviceRows.getOffers(), topK);
            rows.setOffers(offersList);
            log.info("✅ [SERVICE-MAP-12] Mapped: service.rows.offers → dto.rows.offers ({} items)", offersList.size());
        } else {
            // MUST guarantee all arrays are present
            rows.setRecommendedForYou(Collections.emptyList());
            rows.setPopular(Collections.emptyList());
            rows.setOffers(Collections.emptyList());
            log.warn("⚠️ [SERVICE-MAP-10-12] Service rows NULL - using empty arrays");
        }
        dto.setRows(rows);

        // Map meta: determine userSegment based on isColdStart
        log.info("─────────────────────────────────────────────────────────────");
        log.info("🎯 USER SEGMENTATION LOGIC:");
        log.info("─────────────────────────────────────────────────────────────");

        RecommendationMetaDTO meta = new RecommendationMetaDTO();
        if (serviceMeta != null) {
            // Derive history length from isColdStart and numRecommendedForYou
            // Per spec: isColdStart = true if purchase count < 3
            // So: isColdStart = false means historyLen >= 3
            Boolean isColdStart = serviceMeta.getIsColdStart();
            Integer numRecommended = serviceMeta.getNumRecommendedForYou();
            int minHistoryThreshold = serviceMeta.getMinHistoryForPersonalization() != null
                    ? serviceMeta.getMinHistoryForPersonalization() : 3;

            int historyLen;
            String userSegment;

            if (Boolean.FALSE.equals(isColdStart)) {
                // User has >= minHistoryThreshold purchases (e.g., >= 3)
                historyLen = Math.max(minHistoryThreshold, numRecommended != null ? numRecommended : minHistoryThreshold);
                userSegment = "existing";
                log.info("🔍 [SERVICE-MAP-13] isColdStart=false → User is WARM/STALE (historyLen >= {})", minHistoryThreshold);
            } else {
                // User has < minHistoryThreshold purchases (e.g., < 3)
                historyLen = 0;
                userSegment = "new";
                log.info("🔍 [SERVICE-MAP-13] isColdStart=true → User is NEW (historyLen < {})", minHistoryThreshold);
            }

            log.info("🔍 [SERVICE-MAP-14] Business Rule: isColdStart={} → userSegment='{}', historyLen={}",
                    isColdStart, userSegment, historyLen);

            meta.setUserSegment(userSegment);
            meta.setHistoryLen(historyLen);

            log.info("✅ [SERVICE-MAP-15] Final Meta - userSegment: '{}', historyLen: {}", userSegment, historyLen);
        } else {
            // Default values if meta is null
            meta.setUserSegment("new");
            meta.setHistoryLen(0);
            log.warn("⚠️ [SERVICE-MAP-13-15] Service meta NULL - using defaults: userSegment='new', historyLen=0");
        }
        dto.setMeta(meta);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  MAPPING COMPLETED                                           ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        return dto;
    }

    private List<RecommendationItemDTO> mapItems(List<RecommendationServiceItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(this::mapItem).collect(Collectors.toList());
    }

    private RecommendationItemDTO mapItem(RecommendationServiceItem item) {
        RecommendationItemDTO dto = new RecommendationItemDTO();
        dto.setProductId(item.getProductId());
        dto.setName(item.getName());
        dto.setCategoryName(item.getCategoryName());
        dto.setScore(item.getScore());
        dto.setHasOffer(item.getHasOffer());
        if (item.getOffer() != null) {
            OfferInfoDTO offer = new OfferInfoDTO();
            offer.setDiscountPercent(item.getOffer().getDiscountPercent());
            offer.setOfferStrength(item.getOffer().getOfferStrength());
            dto.setOffer(offer);
        }
        dto.setBaseScore(item.getBaseScore());
        dto.setOfferBoost(item.getOfferBoost());

        // Map image data if present
        dto.setImageUrl(item.getImageUrl());
        dto.setImageAlt(item.getImageAlt());

        return dto;
    }

    /**
     * Filter offers list to include only products that actually have active offers in database,
     * and complete the list from database if needed.
     *
     * @param serviceOffers List from recommendation service
     * @param topK Target number of items
     * @return Filtered and completed list of products with offers
     */
    private List<RecommendationItemDTO> filterAndCompleteOffers(List<RecommendationServiceItem> serviceOffers, int topK) {
        log.debug("Starting filterAndCompleteOffers with {} items from service, topK={}",
                  serviceOffers != null ? serviceOffers.size() : 0, topK);

        List<RecommendationItemDTO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Verify each product from recommendation service has an active offer in database
        if (serviceOffers != null && !serviceOffers.isEmpty()) {
            for (RecommendationServiceItem serviceItem : serviceOffers) {
                if (serviceItem.getProductId() == null) {
                    continue;
                }

                // Check if this product has an active offer in database
                boolean hasActiveOffer = checkProductHasActiveOffer(serviceItem.getProductId(), now);

                if (hasActiveOffer) {
                    RecommendationItemDTO item = mapItem(serviceItem);
                    result.add(item);
                    log.debug("Product {} has active offer - keeping in list", serviceItem.getProductId());
                } else {
                    log.debug("Product {} has NO active offer - removing from list", serviceItem.getProductId());
                }
            }

            log.info("Verified products from recommendation service: {} out of {} have active offers in database",
                     result.size(), serviceOffers.size());
        }

        // Step 2: If less than topK, complete from database
        if (result.size() < topK) {
            int needed = topK - result.size();
            log.info("Need {} more items with offers to reach topK={}", needed, topK);

            List<RecommendationItemDTO> additionalOffers = getProductsWithActiveOffers(result, needed);
            result.addAll(additionalOffers);

            log.info("Added {} additional products with offers from database", additionalOffers.size());
        }

        // Step 3: Limit to topK
        if (result.size() > topK) {
            result = result.subList(0, topK);
        }

        log.info("Final offers list size: {} (verified from database)", result.size());
        return result;
    }

    /**
     * Check if a product has any active offer in database.
     *
     * @param productId Product ID to check
     * @param now Current timestamp
     * @return true if product has at least one active offer
     */
    private boolean checkProductHasActiveOffer(Long productId, LocalDateTime now) {
        // Check PRODUCT type offers
        List<Offer> productOffers = offerRepository.findActiveProductOffersForProduct(productId, now);
        if (!productOffers.isEmpty()) {
            return true;
        }

        // Check CATEGORY type offers
        List<Offer> categoryOffers = offerRepository.findActiveCategoryOffersForProduct(productId, now);
        return !categoryOffers.isEmpty();
    }

    /**
     * Get products with active offers from database.
     * Excludes products already in the result list.
     *
     * @param existingItems Items already in the result
     * @param limit Maximum number of items to fetch
     * @return List of products with active offers
     */
    private List<RecommendationItemDTO> getProductsWithActiveOffers(List<RecommendationItemDTO> existingItems, int limit) {
        LocalDateTime now = LocalDateTime.now();

        // Get all active offers
        List<Offer> activeOffers = offerRepository.findActiveOffers(now);

        if (activeOffers.isEmpty()) {
            log.warn("No active offers found in database");
            return Collections.emptyList();
        }

        log.debug("Found {} active offers in database", activeOffers.size());

        // Collect product IDs already in result to avoid duplicates
        List<Long> existingProductIds = existingItems.stream()
                .map(RecommendationItemDTO::getProductId)
                .collect(Collectors.toList());

        List<RecommendationItemDTO> additionalItems = new ArrayList<>();

        // Process each offer to extract products
        for (Offer offer : activeOffers) {
            if (additionalItems.size() >= limit) {
                break;
            }

            // Get products from PRODUCT type offers
            if (offer.getOfferType() == Offer.OfferType.PRODUCT && offer.getOfferProducts() != null) {
                for (var offerProduct : offer.getOfferProducts()) {
                    if (additionalItems.size() >= limit) {
                        break;
                    }

                    Product product = offerProduct.getProduct();
                    if (product != null &&
                        product.getIsActive() &&
                        !existingProductIds.contains(product.getId())) {

                        RecommendationItemDTO item = createItemFromProduct(product, offer);
                        additionalItems.add(item);
                        existingProductIds.add(product.getId());
                    }
                }
            }

            // Get products from CATEGORY type offers
            if (offer.getOfferType() == Offer.OfferType.CATEGORY && offer.getOfferCategories() != null) {
                for (var offerCategory : offer.getOfferCategories()) {
                    if (additionalItems.size() >= limit) {
                        break;
                    }

                    var category = offerCategory.getCategory();
                    if (category != null) {
                        // Get products in this category from database
                        List<Product> categoryProducts = productRepository.findProductsByCategoryId(category.getId());

                        for (Product product : categoryProducts) {
                            if (additionalItems.size() >= limit) {
                                break;
                            }

                            if (product != null &&
                                product.getIsActive() &&
                                !existingProductIds.contains(product.getId())) {

                                RecommendationItemDTO item = createItemFromProduct(product, offer);
                                additionalItems.add(item);
                                existingProductIds.add(product.getId());
                            }
                        }
                    }
                }
            }
        }

        return additionalItems;
    }

    /**
     * Create RecommendationItemDTO from Product and Offer.
     *
     * @param product Product entity
     * @param offer Offer entity
     * @return RecommendationItemDTO
     */
    private RecommendationItemDTO createItemFromProduct(Product product, Offer offer) {
        RecommendationItemDTO item = new RecommendationItemDTO();
        item.setProductId(product.getId());
        item.setName(product.getName());

        // Get category name (first category if multiple)
        String categoryName = null;
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            var firstCategory = product.getCategories().iterator().next();
            if (firstCategory != null) {
                categoryName = firstCategory.getName();
            }
        }
        item.setCategoryName(categoryName);

        // Get primary image or first available image
        if (product.getProductMedia() != null && !product.getProductMedia().isEmpty()) {
            // Try to find primary image first
            ProductMedia primaryImage = product.getProductMedia().stream()
                    .filter(pm -> pm.getIsPrimary() != null && pm.getIsPrimary())
                    .findFirst()
                    .orElse(null);

            // If no primary, get first image sorted by sortOrder
            if (primaryImage == null) {
                primaryImage = product.getProductMedia().stream()
                        .min((pm1, pm2) -> {
                            int order1 = pm1.getSortOrder() != null ? pm1.getSortOrder() : 999;
                            int order2 = pm2.getSortOrder() != null ? pm2.getSortOrder() : 999;
                            return Integer.compare(order1, order2);
                        })
                        .orElse(null);
            }

            if (primaryImage != null && primaryImage.getMedia() != null) {
                item.setImageUrl(primaryImage.getMedia().getUrl());
                item.setImageAlt(primaryImage.getMedia().getAltText() != null
                        ? primaryImage.getMedia().getAltText()
                        : product.getName());
            }
        }

        // Default score for database items
        item.setScore(0.75);
        item.setBaseScore(0.75);

        // Set offer information
        item.setHasOffer(true);

        OfferInfoDTO offerInfo = new OfferInfoDTO();
        if (offer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
            offerInfo.setDiscountPercent(offer.getDiscountValue().intValue());
        } else {
            // For fixed amount, calculate approximate percentage based on product price
            if (product.getDefaultPrice() != null && product.getDefaultPrice().doubleValue() > 0) {
                int approxPercent = (int) ((offer.getDiscountValue().doubleValue() / product.getDefaultPrice().doubleValue()) * 100);
                offerInfo.setDiscountPercent(Math.min(approxPercent, 100));
            } else {
                offerInfo.setDiscountPercent(10); // Default
            }
        }
        offerInfo.setOfferStrength(0.8);
        item.setOffer(offerInfo);

        item.setOfferBoost(0.2);

        return item;
    }

    /**
     * Create error response when recommendation service fails.
     * Returns status="error" with empty arrays for all three row types.
     * Frontend should handle this gracefully.
     *
     * @return Error response with empty data
     */
    private RecommendationsResponseDTO createErrorResponse() {
        log.info("Creating error response - recommendation service unavailable");

        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus("error");

        // MUST guarantee all arrays are present, even in error state
        RecommendationRowsDTO rows = new RecommendationRowsDTO();
        rows.setRecommendedForYou(Collections.emptyList());
        rows.setPopular(Collections.emptyList());
        rows.setOffers(Collections.emptyList());
        dto.setRows(rows);

        // Meta with defaults for error state
        RecommendationMetaDTO meta = new RecommendationMetaDTO();
        meta.setUserSegment("new");
        meta.setHistoryLen(0);
        dto.setMeta(meta);

        return dto;
    }
}
