package com.example.back_end.modules.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for recommendations response.
 * Frontend expects: userSegment ("existing" | "new") and historyLen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationMetaDTO {

    /**
     * User segment: "existing" or "new"
     * - "existing": history_len >= threshold (e.g., 3)
     * - "new": history_len < threshold
     */
    private String userSegment;

    /**
     * Length of user's purchase history
     * Used to determine user segment
     */
    private int historyLen;
}

