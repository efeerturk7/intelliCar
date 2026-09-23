package com.efeerturk.intelliCar.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CarImageRequest(
        @NotBlank
        @URL
        String imageUrl,
        boolean isPrimary
) {
}
