package com.efeerturk.intelliCar.ai;

import java.util.List;


public record AiRecommendationResult(
        String summary,
        List<Pick> picks
) {
    public record Pick(
            String carId,
            Integer score,
            String reason,
            List<String> pros,
            List<String> cons
    ) {
    }
}
