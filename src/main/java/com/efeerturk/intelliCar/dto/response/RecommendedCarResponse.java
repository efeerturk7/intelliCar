package com.efeerturk.intelliCar.dto.response;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Önerilen tek bir araç. Araç bilgileri (fiyat, yıl, km...) veritabanından gelir;
 * yapay zekadan sadece score, reason, pros ve cons alınır.
 */
public record RecommendedCarResponse(
        int rank,
        int score,
        UUID carId,
        String brand,
        String model,
        int year,
        int mileage,
        BigDecimal price,
        FuelType fuelType,
        Transmission transmission,
        String city,
        String primaryImageUrl,
        String reason,
        List<String> pros,
        List<String> cons
) {
}
