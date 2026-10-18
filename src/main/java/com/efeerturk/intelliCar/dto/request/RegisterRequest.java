package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,
        // DÜZELTME: @Min String'de çalışmaz, @Size kullanılmalı.
        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String phone,
        // DÜZELTME: @NotBlank enum'da çalışmaz, @NotNull kullanılmalı.
        @NotNull
        UserRole role
) {
}
