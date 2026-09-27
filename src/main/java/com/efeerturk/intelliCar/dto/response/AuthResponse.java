package com.efeerturk.intelliCar.dto.response;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,

        UUID userId,
        String email,
        String role


) {
}
