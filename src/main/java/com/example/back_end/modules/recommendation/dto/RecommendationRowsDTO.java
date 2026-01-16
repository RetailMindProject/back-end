package com.example.back_end.modules.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationRowsDTO {

    private List<RecommendationItemDTO> forYou;
    private List<RecommendationItemDTO> popular;
    private List<RecommendationItemDTO> offers;
}

