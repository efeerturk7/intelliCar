package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CarImageService {
    void deleteImage(UUID imageId, UUID sellerId);
    void setPrimaryImage(UUID carId,UUID imageId,UUID sellerId);
    List<CarImageResponse> addImagesToCar(UUID carId, List<CarImageRequest> requestList, UUID sellerId);

    /**
     * Cihazdan seçilen görsel dosyalarını kaydeder ve ilana ekler.
     *
     * @param baseUrl      dosya adresinin başına eklenir (örn. http://localhost:8080)
     * @param firstPrimary true ise ilk dosya kapak görseli olur; ilanın hiç kapak görseli yoksa da ilk dosya kapak olur
     */
    List<CarImageResponse> uploadImages(UUID carId, List<MultipartFile> files, boolean firstPrimary, UUID sellerId, String baseUrl);
}
