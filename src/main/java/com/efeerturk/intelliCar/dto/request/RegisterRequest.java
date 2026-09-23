package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Min(6)
        String password,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String phone,
        @NotBlank
        UserRole role
) {
}
