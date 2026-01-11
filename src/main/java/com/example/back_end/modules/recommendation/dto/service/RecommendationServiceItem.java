package com.example.back_end.modules.recommendation.dto.service;

import lombok.Data;

@Data
public class RecommendationServiceItem {

    private Long productId;
    private String name;
    private String categoryName;
    private Double score;
    private Boolean hasOffer;
    private RecommendationServiceOffer offer;
    private Double baseScore;
    private Double offerBoost;
}

