package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CarFilterRequest(
        @NotBlank
        String brand,
        @NotBlank
        String model,

        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        Integer maxYear,
        FuelType fuelType,
        Transmission transmission,
        @NotBlank
        String city
) {
}
