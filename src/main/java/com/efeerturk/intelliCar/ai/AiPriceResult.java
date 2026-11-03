package com.efeerturk.intelliCar.ai;

import java.util.List;

/**
 * Gemini'den beklediğimiz yapılandırılmış (JSON) fiyat cevabı. Fiyatlar TL cinsinden tam sayıdır.
 * Hiçbir değere doğrudan güvenilmez; PriceMarketAnalyzer.isPlausible ile denetlenir.
 */
public record AiPriceResult(
        Long minPrice,
        Long suggestedPrice,
        Long maxPrice,
        String explanation,
        List<String> factors
) {
}
