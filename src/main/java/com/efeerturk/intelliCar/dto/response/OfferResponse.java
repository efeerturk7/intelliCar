package com.efeerturk.intelliCar.dto.response;

import com.efeerturk.intelliCar.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID carId,
        String carTitle,
        BigDecimal offeredPrice,
        OfferStatus status,
        String note,
        UserResponse buyer,
        Instant createdAt
) {
}
