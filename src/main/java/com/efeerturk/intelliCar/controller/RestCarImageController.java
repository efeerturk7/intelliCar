package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.service.CarImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/carImage")
public class RestCarImageController {

    private final CarImageService carImageService;

    /** Boşsa adres, isteğin geldiği sunucudan üretilir (örn. http://localhost:8080). */
    @Value("${intellicar.upload.public-base-url:}")
    private String publicBaseUrl;

    /** Görselin internetteki adresi (URL) biliniyorsa onu ilana ekler. */
    @PostMapping("/{carId}")
    public ResponseEntity<List<CarImageResponse>> addImagesToCar(
            @PathVariable UUID carId,
            @Valid @RequestBody List<CarImageRequest> requestList,
            @AuthenticationPrincipal User currentUser
    ) {
        List<CarImageResponse> responses = carImageService.addImagesToCar(carId, requestList, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Cihazdan seçilen görselleri yükler (multipart/form-data). Alan adı fark etmez (file, files, image...),
     * istekteki tüm dosyalar alınır. isPrimary=true ise ilk dosya kapak görseli olur.
     */
    @PostMapping(value = "/{carId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<CarImageResponse>> uploadImages(
            @PathVariable UUID carId,
            MultipartRequest multipartRequest,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            @AuthenticationPrincipal User currentUser
    ) {
        List<MultipartFile> files = multipartRequest.getMultiFileMap().values().stream()
                .flatMap(List::stream)
                .toList();
        List<CarImageResponse> responses = carImageService.uploadImages(
                carId, files, isPrimary, currentUser.getId(), baseUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PatchMapping("/{carId}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable UUID carId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal User currentUser
    ) {
        carImageService.setPrimaryImage(carId, imageId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID imageId,
            @AuthenticationPrincipal User currentUser
    ) {
        carImageService.deleteImage(imageId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    private String baseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
