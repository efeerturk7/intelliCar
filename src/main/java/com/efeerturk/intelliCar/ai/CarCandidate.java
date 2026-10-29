package com.efeerturk.intelliCar.ai;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.BigDecimal;
import java.util.UUID;


public record CarCandidate(
        UUID id,
        String brand,
        String model,
        int year,
        int mileage,
        BigDecimal price,
        FuelType fuelType,
        Transmission transmission,
        String city,
        String description,
        String primaryImageUrl
) {
}
