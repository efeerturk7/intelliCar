package com.efeerturk.intelliCar.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferCreateRequest(
        @NotNull
        UUID carId,
        @NotNull
        @Positive
        BigDecimal offeredPrice,
        //optional
        String note
) {
}
