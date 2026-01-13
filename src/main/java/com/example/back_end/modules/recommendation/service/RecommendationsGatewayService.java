package com.example.back_end.modules.recommendation.service;

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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationsGatewayService {

    private final WebClient recommendationWebClient;

    public RecommendationsResponseDTO getRecommendations(Long customerId,
                                                         String bearerToken,
                                                         int topK,
                                                         int candidateLimit,
                                                         boolean inStockOnly) {
        try {
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
                    .block();

            if (serviceResponse == null) {
                log.error("Recommendation service returned null body");
                return createFallback(customerId, topK);
            }

            return mapToFrontend(customerId, serviceResponse);
        } catch (Exception ex) {
            log.error("Failed to fetch recommendations for customer {}: {}", customerId, ex.getMessage(), ex);
            return createFallback(customerId, topK);
        }
    }

    private RecommendationsResponseDTO mapToFrontend(Long customerId, RecommendationServiceResponse serviceResponse) {
        // Logging for debugging empty recommendations
        log.info("=== Recommendation Service Response for customer {} ===", customerId);
        log.info("Status: {}", serviceResponse.getStatus());

        RecommendationServiceRows serviceRows = serviceResponse.getRows();
        if (serviceRows != null) {
            int forYouCount = serviceRows.getForYou() != null ? serviceRows.getForYou().size() : 0;
            int popularCount = serviceRows.getPopular() != null ? serviceRows.getPopular().size() : 0;
            int offersCount = serviceRows.getOffers() != null ? serviceRows.getOffers().size() : 0;

            log.info("Rows - For You: {}, Popular: {}, Offers: {}", forYouCount, popularCount, offersCount);

            if (serviceRows.getForYou() != null && serviceRows.getForYou().isEmpty()) {
                log.warn("⚠️  WARNING: for_you array is EMPTY for customer {}", customerId);
            }
        } else {
            log.warn("⚠️  WARNING: serviceRows is NULL for customer {}", customerId);
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
            log.warn("⚠️  WARNING: serviceMeta is NULL for customer {}", customerId);
        }
        log.info("=== End of Recommendation Service Response ===");

        RecommendationsResponseDTO dto = new RecommendationsResponseDTO();
        dto.setStatus(serviceResponse.getStatus());
        dto.setUserId(customerId);

        RecommendationRowsDTO rows = new RecommendationRowsDTO();
        if (serviceRows != null) {
            rows.setForYou(mapItems(serviceRows.getForYou()));
            rows.setPopular(mapItems(serviceRows.getPopular()));
            rows.setOffers(mapItems(serviceRows.getOffers()));
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
