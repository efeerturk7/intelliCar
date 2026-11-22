package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.response.FileUploadResponse;
import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Genel dosya yükleme: görseli yükler ve adresini döner. İlana eklemek için dönen adres
 * POST /carImage/{carId} ile gönderilir ([{"imageUrl": "...", "isPrimary": true}]).
 * Giriş gerektirir. Dosya türü ve boyut kontrolü ImageStorage içindedir.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/files")
public class RestFileController {

    private static final int MAX_FILES_PER_REQUEST = 10;

    private final ImageStorage imageStorage;

    @Value("${intellicar.upload.public-base-url:}")
    private String publicBaseUrl;

    /** Form alan adı fark etmez (file, files, image...). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(MultipartRequest multipartRequest) {
        List<MultipartFile> files = multipartRequest.getMultiFileMap().values().stream()
                .flatMap(List::stream)
                .toList();
        if (files.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.INVALID_IMAGE_FILE, "Dosya seçilmedi"));
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new BaseException(new ErrorMessage(MessageType.IMAGE_LIMIT_EXCEEDED,
                    "Tek seferde en fazla " + MAX_FILES_PER_REQUEST + " dosya yüklenebilir"));
        }

        String base = baseUrl();
        List<String> stored = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                stored.add(imageStorage.store(file));
            }
        } catch (RuntimeException ex) {
            stored.forEach(imageStorage::deleteQuietly);
            throw ex;
        }
        List<String> urls = stored.stream().map(path -> base + path).toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FileUploadResponse(urls.get(0), urls.get(0), urls));
    }

    private String baseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "");
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
