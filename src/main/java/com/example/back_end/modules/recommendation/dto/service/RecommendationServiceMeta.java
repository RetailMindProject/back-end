package com.example.back_end.modules.recommendation.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecommendationServiceMeta {

    @JsonProperty("topK")
    private Integer topK;

    @JsonProperty("candidateLimit")
    private Integer candidateLimit;

    @JsonProperty("numRecommendedForYou")
    private Integer numRecommendedForYou;

    @JsonProperty("numPopular")
    private Integer numPopular;

    @JsonProperty("numOffers")
    private Integer numOffers;

    @JsonProperty("userSegment")
    private String userSegment; // "warm", "stale", "new_user"

    @JsonProperty("isColdStart")
    private Boolean isColdStart;

    @JsonProperty("isStale")
    private Boolean isStale;

    @JsonProperty("daysSinceLastEvent")
    private Integer daysSinceLastEvent;

    @JsonProperty("minHistoryForPersonalization")
    private Integer minHistoryForPersonalization;

    @JsonProperty("staleDays")
    private Integer staleDays;

    @JsonProperty("recencyTauDays")
    private Double recencyTauDays;

    @JsonProperty("categoryAffinityBeta")
    private Double categoryAffinityBeta;

    @JsonProperty("popularityBlendDisabled")
    private Boolean popularityBlendDisabled;

    @JsonProperty("minOffersK")
    private Integer minOffersK;
}

