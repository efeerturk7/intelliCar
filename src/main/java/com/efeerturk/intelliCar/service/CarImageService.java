package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;

import java.util.List;
import java.util.UUID;

public interface CarImageService {
    void deleteImage(UUID imageId, UUID sellerId);
    void setPrimaryImage(UUID carId,UUID imageId,UUID sellerId);
    List<CarImageResponse> addImagesToCar(UUID carId, List<CarImageRequest> requestList, UUID sellerId);
}
