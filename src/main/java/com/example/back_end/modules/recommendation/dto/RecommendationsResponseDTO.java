package com.example.back_end.modules.recommendation.dto;

import lombok.Data;

@Data
public class RecommendationsResponseDTO {

    private String status; // success or error
    private RecommendationRowsDTO rows;
    private RecommendationMetaDTO meta;
}

