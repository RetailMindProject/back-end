package com.example.back_end.modules.recommendation.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecommendationServiceMeta {

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("num_for_you")
    private Integer numForYou;

    @JsonProperty("num_popular")
    private Integer numPopular;

    @JsonProperty("num_offers")
    private Integer numOffers;

    @JsonProperty("is_cold_start")
    private Boolean isColdStart;

    @JsonProperty("is_stale")
    private Boolean isStale;

    @JsonProperty("user_segment")
    private String userSegment;
}

