package com.example.back_end.modules.recommendation.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationServiceRows {

    @JsonProperty("for_you")
    private List<RecommendationServiceItem> forYou;

    private List<RecommendationServiceItem> popular;

    private List<RecommendationServiceItem> offers;
}

