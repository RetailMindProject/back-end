package com.example.back_end.modules.recommendation.service;

import com.example.back_end.modules.recommendation.dto.CustomerDTO;
import com.example.back_end.modules.recommendation.dto.ProductCandidateDTO;
import com.example.back_end.modules.recommendation.dto.ProductCatalogDTO;
import com.example.back_end.modules.recommendation.dto.ProductOfferEnrichmentDTO;
import com.example.back_end.modules.recommendation.dto.PurchaseEventDTO;
import com.example.back_end.modules.recommendation.dto.TrendingProductDTO;
import com.example.back_end.modules.recommendation.dto.UserHistoryItemDTO;
import com.example.back_end.modules.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;

    public List<UserHistoryItemDTO> getUserHistory(Long customerId, int limit) {
        return recommendationRepository.findUserHistory(customerId, limit);
    }

    public List<ProductCandidateDTO> getProductCandidates(int limit, boolean inStockOnly) {
        return recommendationRepository.findProductCandidates(limit, inStockOnly);
    }

    public List<TrendingProductDTO> getTrending(int days, int limit) {
        return recommendationRepository.findTrending(days, limit);
    }

    public List<PurchaseEventDTO> getPurchaseEventsForTraining(int limit, int offset) {
        return recommendationRepository.findPurchaseEventsForTraining(limit, offset);
    }

    public List<ProductCatalogDTO> getProductCatalogForTraining(int limit, int offset) {
        return recommendationRepository.findProductCatalogForTraining(limit, offset);
    }

    public List<CustomerDTO> getCustomersForTraining(int limit, int offset) {
        return recommendationRepository.findCustomersForTraining(limit, offset);
    }

    public List<ProductOfferEnrichmentDTO> getActiveOffersByProductIds(List<Long> productIds) {
        return recommendationRepository.findActiveOffersByProductIds(productIds);
    }

    public List<ProductOfferEnrichmentDTO> getActiveOffersPaginated(int limit, int offset) {
        return recommendationRepository.findActiveOffersPaginated(limit, offset);
    }
}
