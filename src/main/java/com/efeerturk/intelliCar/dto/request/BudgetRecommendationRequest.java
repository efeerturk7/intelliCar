package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record BudgetRecommendationRequest(
        @NotNull(message = "Budget is required")
        @Positive(message = "Budget must be greater than zero")
        BigDecimal budget,

        // Serbest metin tercih, örn: "aile için geniş, düşük yakıt tüketimi"
        @Size(max = 500, message = "Preferences can be at most 500 characters")
        String preferences,

        FuelType fuelType,

        Transmission transmission,

        @Size(max = 100, message = "City can be at most 100 characters")
        String city,

        // Kaç araç önerilsin (varsayılan 5)
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 10, message = "Limit can be at most 10")
        Integer limit
) {
}
