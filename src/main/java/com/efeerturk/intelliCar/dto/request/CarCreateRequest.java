package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CarCreateRequest(
        @NotBlank
        String brand,
        @NotBlank
        String model,
        @Min(1900)
        int year,
        @PositiveOrZero
        int mileage,
        @NotNull
        @Positive
        BigDecimal price,
        @NotNull
        FuelType fuelType,
        @NotNull
        Transmission transmission,
        @NotBlank
        String city,
        @NotBlank
        String description,
        List<CarImageRequest> images
) {
}
