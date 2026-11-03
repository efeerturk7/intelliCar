package com.efeerturk.intelliCar.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


public record PriceEstimateResponse(
        BigDecimal minPrice,
        BigDecimal suggestedPrice,
        BigDecimal maxPrice,
        String currency,
        Confidence confidence,
        boolean aiAssisted,
        String explanation,
        List<String> factors,
        MarketStats market,
        List<ComparableCarResponse> comparables,
        String disclaimer
) {

    /** Güven düzeyi, aynı modelden kaç benzer ilan bulunduğuna göre kodda belirlenir (modele sorulmaz). */
    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    public record MarketStats(
            int comparablesCount,
            int sameModelCount,
            BigDecimal min,
            BigDecimal median,
            BigDecimal max
    ) {
    }

    public record ComparableCarResponse(
            UUID carId,
            String brand,
            String model,
            int year,
            int mileage,
            BigDecimal price,
            String city
    ) {
    }
}
