package com.efeerturk.intelliCar.dto.response;

import com.efeerturk.intelliCar.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        Instant createdAt
) {
}
