package com.efeerturk.intelliCar.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BudgetRecommendationResponse(
        BigDecimal budget,
        // Yapay zekanın incelediği ilan sayısı
        int candidatesAnalyzed,
        String summary,
        List<RecommendedCarResponse> recommendations
) {
}
