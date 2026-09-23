package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.OfferStatus;
import jakarta.validation.constraints.NotNull;

public record OfferStatusUpdateRequest(
        @NotNull
        OfferStatus status
) {
}
