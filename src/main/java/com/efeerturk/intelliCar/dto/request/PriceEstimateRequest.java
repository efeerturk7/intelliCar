package com.efeerturk.intelliCar.dto.request;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Satmak istenen aracın bilgileri. description alanı aracın durumunu serbestçe anlatmak içindir
 * (örn. "boyasız, hasar kaydı yok, düzenli bakımlı").
 */
public record PriceEstimateRequest(
        @NotBlank(message = "Brand is required")
        @Size(max = 50, message = "Brand can be at most 50 characters")
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 50, message = "Model can be at most 50 characters")
        String model,

        @Min(value = 1900, message = "Year must be 1900 or later")
        @Max(value = 2100, message = "Year is not valid")
        int year,

        @PositiveOrZero(message = "Mileage cannot be negative")
        int mileage,

        @NotNull(message = "Fuel type is required")
        FuelType fuelType,

        @NotNull(message = "Transmission is required")
        Transmission transmission,

        @Size(max = 100, message = "City can be at most 100 characters")
        String city,

        @Size(max = 1000, message = "Description can be at most 1000 characters")
        String description
) {
}
