package com.efeerturk.intelliCar.dto.response;

import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CarDetailResponse(
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
        CarStatus status,
        long viewCount,
        SellerSummaryResponse seller,
        List<CarImageResponse> images,
        Instant createdAt
) {
}
