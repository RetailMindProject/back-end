package com.example.back_end.modules.recommendation.dto.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecommendationServiceResponse {

    private String status;

    @JsonProperty("user_id")
    private Long userId;

    private RecommendationServiceRows rows;

    private RecommendationServiceMeta meta;
}

