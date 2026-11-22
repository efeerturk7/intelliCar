package com.efeerturk.intelliCar.ai;

import java.util.List;

/**
 * Modelden beklediğimiz yapılandırılmış (JSON) cevap.
 * Spring AI bu sınıfın şemasını modele gönderir ve cevabı otomatik bu sınıfa çevirir.
 * Modelden gelen hiçbir değer doğrudan güvenilmez; servis katmanında doğrulanır.
 */
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
