package com.efeerturk.intelliCar.dto.response;

import java.util.UUID;

public record SellerSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone
) {
}
