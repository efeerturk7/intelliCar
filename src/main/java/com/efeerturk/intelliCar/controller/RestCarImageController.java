package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.service.CarImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/carImage")
public class RestCarImageController {
    private final CarImageService carImageService;
    @PostMapping("/{carId}")
    public ResponseEntity<List<CarImageResponse>> addImagesToCar(
            @PathVariable UUID carId,
            @Valid @RequestBody List<CarImageRequest> requestList,
            @RequestHeader("X-User-Id") UUID sellerId
    ) {
        List<CarImageResponse> responses = carImageService.addImagesToCar(carId, requestList, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }


    @PatchMapping("/{carId}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable UUID carId,
            @PathVariable UUID imageId,
            @RequestHeader("X-User-Id") UUID sellerId
    ) {
        carImageService.setPrimaryImage(carId, imageId, sellerId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID imageId,
            @RequestHeader("X-User-Id") UUID sellerId
    ) {
        carImageService.deleteImage(imageId, sellerId);
        return ResponseEntity.noContent().build();
    }
}
