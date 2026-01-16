package com.example.back_end.modules.recommendation.dto;

import lombok.Data;

@Data
public class RecommendationItemDTO {

    private Long productId;
    private String name;
    private String categoryName;
    private Double score;
    private Boolean hasOffer;
    private OfferInfoDTO offer;
    private Double baseScore;
    private Double offerBoost;
}

