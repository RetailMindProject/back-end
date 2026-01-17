package com.example.back_end.modules.recommendation.service;

import com.example.back_end.modules.catalog.product.entity.Product;
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
                log.warn("Recommendation service unavailable or returned null - using database fallback for customer {}", customerId);
                return createFallbackWithDatabaseOffers(customerId, topK);
            }

            return mapToFrontend(customerId, serviceResponse);
        } catch (Exception ex) {
            log.warn("Failed to fetch recommendations for customer {}: {} - using database fallback",
                     customerId, ex.getMessage());
            return createFallbackWithDatabaseOffers(customerId, topK);
        }
    }

    private RecommendationsResponseDTO mapToFrontend(Long customerId, RecommendationServiceResponse serviceResponse) {
        // Logging for debugging empty recommendations
        log.info("=== Recommendation Service Response for customerId {} (from customers table) ===", customerId);
        log.info("Status: {}", serviceResponse.getStatus());

        RecommendationServiceRows serviceRows = serviceResponse.getRows();
        if (serviceRows != null) {
            int forYouCount = serviceRows.getForYou() != null ? serviceRows.getForYou().size() : 0;
            int popularCount = serviceRows.getPopular() != null ? serviceRows.getPopular().size() : 0;
            int offersCount = serviceRows.getOffers() != null ? serviceRows.getOffers().size() : 0;

            log.info("Rows - For You: {}, Popular: {}, Offers: {}", forYouCount, popularCount, offersCount);

            if (serviceRows.getForYou() != null && serviceRows.getForYou().isEmpty()) {
                log.warn("⚠️  WARNING: for_you array is EMPTY for customerId {}", customerId);
            }
        } else {
            log.warn("⚠️  WARNING: serviceRows is NULL for customerId {}", customerId);
        }

        RecommendationServiceMeta serviceMeta = serviceResponse.getMeta();
        if (serviceMeta != null) {
            log.info("Meta - Cold Start: {}, Stale: {}, User Segment: {}",
                     serviceMeta.getIsColdStart(),
                     serviceMeta.getIsStale(),
                     serviceMeta.getUserSegment());
            log.info("Meta - Counts - For You: {}, Popular: {}, Offers: {}",
                     serviceMeta.getNumForYou(),
                     serviceMeta.getNumPopular(),
                     serviceMeta.getNumOffers());
        } else {
            log.warn("⚠️  WARNING: serviceMeta is NULL for customerId {}", customerId);
        }
        log.info("=== End of Recommendation Service Response ===");

        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus(serviceResponse.getStatus());
        dto.setUserId(customerId);

        RecommendationRowsDTO rows = new RecommendationRowsDTO();
        if (serviceRows != null) {
            rows.setForYou(mapItems(serviceRows.getForYou()));
            rows.setPopular(mapItems(serviceRows.getPopular()));

            // معالجة خاصة لقائمة offers: تصفية وإكمال
            int topK = (serviceMeta != null && serviceMeta.getTopK() != null)
                        ? serviceMeta.getTopK()
                        : 10;
            rows.setOffers(filterAndCompleteOffers(serviceRows.getOffers(), topK));
        } else {
            rows.setForYou(Collections.emptyList());
            rows.setPopular(Collections.emptyList());
            rows.setOffers(Collections.emptyList());
        }
        dto.setRows(rows);

        RecommendationMetaDTO meta = new RecommendationMetaDTO();
        if (serviceMeta != null) {
            meta.setTopK(serviceMeta.getTopK());
            meta.setNumForYou(serviceMeta.getNumForYou());
            meta.setNumPopular(serviceMeta.getNumPopular());
            meta.setNumOffers(serviceMeta.getNumOffers());
            meta.setIsColdStart(serviceMeta.getIsColdStart());
            meta.setIsStale(serviceMeta.getIsStale());
            meta.setUserSegment(serviceMeta.getUserSegment());
        }
        dto.setMeta(meta);

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
     * Create fallback response with actual offers from database when recommendation service is unavailable.
     * This provides a better user experience than returning empty lists.
     *
     * @param customerId Customer ID
     * @param topK Target number of items
     * @return Response with database offers
     */
    private RecommendationsResponseDTO createFallbackWithDatabaseOffers(Long customerId, int topK) {
        log.info("Creating fallback response with database offers for customer {}", customerId);

        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus("success");
        dto.setUserId(customerId);
        dto.setMessage(null);

        RecommendationRowsDTO rows = new RecommendationRowsDTO();

        // Get products with active offers from database
        List<RecommendationItemDTO> offersFromDb = getProductsWithActiveOffers(Collections.emptyList(), topK);

        // Use offers for all three categories when service is down
        rows.setForYou(offersFromDb.isEmpty() ? Collections.emptyList() : offersFromDb.subList(0, Math.min(offersFromDb.size(), topK)));
        rows.setPopular(offersFromDb.isEmpty() ? Collections.emptyList() : offersFromDb.subList(0, Math.min(offersFromDb.size(), topK)));
        rows.setOffers(offersFromDb);
        dto.setRows(rows);

        RecommendationMetaDTO meta = new RecommendationMetaDTO();
        meta.setTopK(topK);
        meta.setNumForYou(rows.getForYou().size());
        meta.setNumPopular(rows.getPopular().size());
        meta.setNumOffers(rows.getOffers().size());
        meta.setIsColdStart(true); // Mark as cold start since we're using fallback
        meta.setIsStale(false);
        meta.setUserSegment("fallback");
        dto.setMeta(meta);

        log.info("Fallback response created with {} offers from database", offersFromDb.size());
        return dto;
    }

    private RecommendationsResponseDTO createFallback(Long customerId, int topK) {
        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus("error");
        dto.setUserId(customerId);
        dto.setMessage("Recommendations temporarily unavailable");

        RecommendationRowsDTO rows = new RecommendationRowsDTO();
        rows.setForYou(Collections.emptyList());
        rows.setPopular(Collections.emptyList());
        rows.setOffers(Collections.emptyList());
        dto.setRows(rows);

        RecommendationMetaDTO meta = new RecommendationMetaDTO();
        meta.setTopK(topK);
        meta.setNumForYou(0);
        meta.setNumPopular(0);
        meta.setNumOffers(0);
        meta.setIsColdStart(false);
        meta.setIsStale(false);
        meta.setUserSegment(null);
        dto.setMeta(meta);

        return dto;
    }
}
