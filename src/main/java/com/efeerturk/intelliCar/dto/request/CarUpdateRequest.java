package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.CarStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CarUpdateRequest(
        @NotNull @Positive
        BigDecimal price,
        @PositiveOrZero
        int mileage,
        @NotBlank
        String description,
        @NotNull
        CarStatus status
) {
}
