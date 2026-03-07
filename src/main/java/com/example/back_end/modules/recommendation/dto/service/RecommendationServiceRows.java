package com.example.back_end.modules.recommendation.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationServiceRows {

    @JsonProperty("recommendedForYou")
    private List<RecommendationServiceItem> recommendedForYou;

    private List<RecommendationServiceItem> popular;

    private List<RecommendationServiceItem> offers;
}

