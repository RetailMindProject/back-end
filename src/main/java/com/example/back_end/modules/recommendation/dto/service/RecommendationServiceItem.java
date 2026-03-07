package com.example.back_end.modules.recommendation.dto.service;

import lombok.Data;

@Data
public class RecommendationServiceItem {

    private Long productId;
    private String name;
    private String categoryName;
    private Double score;
    private String source; // "personalized" or "random_user_preferences" (only in recommendedForYou)
    private Boolean hasOffer;
    private RecommendationServiceOffer offer;
    private Double baseScore; // Only in offers
    private Double offerBoost; // Only in offers

    // Image support
    private String imageUrl;
    private String imageAlt;
}

