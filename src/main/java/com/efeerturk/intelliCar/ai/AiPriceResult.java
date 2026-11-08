package com.efeerturk.intelliCar.ai;

import java.util.List;

/**
 * Gemini'den beklediğimiz yapılandırılmış (JSON) fiyat cevabı. Fiyatlar TL cinsinden tam sayıdır.
 * Hiçbir değere doğrudan güvenilmez; PriceMarketAnalyzer.decide ile denetlenir.
 *
 * conditionImpact metin olarak alınır (POSITIVE, NEUTRAL, NEGATIVE, UNKNOWN), böylece beklenmeyen bir değer
 * tüm cevabı çöpe atmaz; ConditionImpact.parse tanınmayan değeri UNKNOWN sayar.
 */
public record AiPriceResult(
        Long minPrice,
        Long suggestedPrice,
        Long maxPrice,
        String conditionImpact,
        String conditionSummary,
        String explanation,
        List<String> factors
) {
}
