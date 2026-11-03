package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.PriceEstimateRequest;
import com.efeerturk.intelliCar.dto.response.PriceEstimateResponse;

import java.util.UUID;

public interface PriceEstimationService {

    /**
     * Aynı markadaki benzer ilanları analiz ederek, satılmak istenen araç için bir fiyat aralığı önerir.
     */
    PriceEstimateResponse estimatePrice(PriceEstimateRequest request, UUID currentUserId);
}
