package com.example.back_end.modules.recommendation.dto;

import lombok.Data;

@Data
public class RecommendationMetaDTO {

    private Integer topK;
    private Integer numForYou;
    private Integer numPopular;
    private Integer numOffers;
    private Boolean isColdStart;
    private Boolean isStale;
    private String userSegment;
}

