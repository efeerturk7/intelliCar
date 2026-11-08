package com.efeerturk.intelliCar.dto.response;

import com.efeerturk.intelliCar.ai.ConditionImpact;
import com.efeerturk.intelliCar.ai.PriceMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Satış fiyatı tahmini.
 *
 * method: baz fiyatın nasıl bulunduğunu gösterir (aynı model ve yıldaki ilanların kilometre karşılaştırması ya da benzer ilanlar).
 * mileageBasedPrice: yalnızca ilanlara ve kilometreye dayanan baz fiyat. suggestedPrice bundan, aracın durumuna
 * (hasar kaydı, değişen, boya) göre sınırlı bir farkla ayrılabilir.
 * aiAssisted=false ise Gemini kullanılamadı (veya tutarsız bir cevap verdi): fiyat yalnızca baz fiyat ve istatistiklerden gelir
 * ve aracın durumu fiyata yansıtılmamıştır.
 */
public record PriceEstimateResponse(
        BigDecimal minPrice,
        BigDecimal suggestedPrice,
        BigDecimal maxPrice,
        String currency,
        Confidence confidence,
        boolean aiAssisted,
        PriceMethod method,
        BigDecimal mileageBasedPrice,
        ConditionAnalysis condition,
        String explanation,
        List<String> factors,
        MarketStats market,
        List<ComparableCarResponse> comparables,
        String disclaimer
) {

    /** Güven düzeyi kodda hesaplanır (modele sorulmaz): yönteme, ilan sayısına ve kilometre aralığına bağlıdır. */
    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    /** Araç açıklamasından anlaşılan durum (hasar kaydı, değişen, boya) ve fiyata etkisi. */
    public record ConditionAnalysis(
            ConditionImpact impact,
            String summary
    ) {
    }

    /** min, median ve max: baz fiyatın dayandığı ilanların fiyatları (aynı model ve yıl varsa yalnızca onlar). */
    public record MarketStats(
            int comparablesCount,
            int sameModelCount,
            int exactMatchCount,
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
            String city,
            boolean exactMatch
    ) {
    }
}
