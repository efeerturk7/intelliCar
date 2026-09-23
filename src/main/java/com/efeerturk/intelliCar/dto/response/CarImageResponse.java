package com.efeerturk.intelliCar.dto.response;

import java.util.UUID;

public record CarImageResponse(
        UUID id,
        String imageUrl,
        boolean isPrimary
) {
}
