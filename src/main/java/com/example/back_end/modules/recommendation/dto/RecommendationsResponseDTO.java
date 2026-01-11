package com.example.back_end.modules.recommendation.dto;

import lombok.Data;

@Data
public class RecommendationsResponseDTO {

    private String status; // success or error
    private Long userId;
    private RecommendationRowsDTO rows;
    private RecommendationMetaDTO meta;
    private String message;
}

