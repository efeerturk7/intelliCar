package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.BigDecimal;

// DÜZELTME: Tüm filtreler opsiyonel. @NotBlank kaldırıldı (zaten @Valid olmadığı için çalışmıyordu,
// ama niyet "zorunlu" gibi görünüyordu; CarSpecification null alanları zaten atlıyor).
public record CarFilterRequest(
        String brand,
        String model,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        Integer maxYear,
        FuelType fuelType,
        Transmission transmission,
        String city
) {
}
