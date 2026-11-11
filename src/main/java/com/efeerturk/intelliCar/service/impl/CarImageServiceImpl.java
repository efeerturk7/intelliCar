package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.mapper.CarImageMapper;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.CarImage;
import com.efeerturk.intelliCar.repository.CarImageRepository;
import com.efeerturk.intelliCar.repository.CarRepository;
import com.efeerturk.intelliCar.service.CarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.efeerturk.intelliCar.storage.ImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CarImageServiceImpl implements CarImageService {
    private final CarImageRepository carImageRepository;
    private final CarRepository carRepository;
    // DÜZELTME: final yapıldı. Önceden final olmadığı için @RequiredArgsConstructor enjekte etmiyor, NPE oluyordu.
    private final CarImageMapper carImageMapper;
    private final ImageStorage imageStorage;

    @Value("${intellicar.upload.max-images-per-car:15}")
    private int maxImagesPerCar;

    @Override
    @Transactional
    public List<CarImageResponse> uploadImages(UUID carId, List<MultipartFile> files, boolean firstPrimary,
                                               UUID sellerId, String baseUrl) {
        if (files == null || files.isEmpty()) {
            throw new BaseException(new ErrorMessage(MessageType.INVALID_IMAGE_FILE, "Dosya seçilmedi"));
        }
        // Dosyaları diske yazmadan önce yetki ve limit kontrolü yapılır
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.CAR_NOT_FOUND, carId.toString())));
        if (!car.getSeller().getId().equals(sellerId)) {
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION, sellerId.toString()));
        }
        List<CarImage> existing = carImageRepository.findByCarId(carId);
        if (existing.size() + files.size() > maxImagesPerCar) {
            throw new BaseException(new ErrorMessage(MessageType.IMAGE_LIMIT_EXCEEDED,
                    "En fazla " + maxImagesPerCar + " görsel eklenebilir"));
        }

        List<String> storedPaths = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                storedPaths.add(imageStorage.store(file));
            }
            boolean hasPrimary = existing.stream().anyMatch(CarImage::isPrimary);
            List<CarImageRequest> requests = new ArrayList<>();
            for (int i = 0; i < storedPaths.size(); i++) {
                boolean primary = i == 0 && (firstPrimary || !hasPrimary);
                requests.add(new CarImageRequest(baseUrl + storedPaths.get(i), primary));
            }
            return addImagesToCar(carId, requests, sellerId);
        } catch (RuntimeException ex) {
            // Veritabanına yazılamadıysa diskte yetim dosya kalmasın
            storedPaths.forEach(imageStorage::deleteQuietly);
            throw ex;
        }
    }

    @Override
    @Transactional
    public List<CarImageResponse> addImagesToCar(UUID carId, List<CarImageRequest> requestList, UUID sellerId) {
        Car dbCar = carRepository.findById(carId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.CAR_NOT_FOUND, carId.toString())));
        if (!dbCar.getSeller().getId().equals(sellerId)) {
            log.error("Bu ilana görsel ekleme yetkiniz yok");
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION, sellerId.toString()));
        }

        // Aynı araçta birden fazla primary olursa findByCarIdAndIsPrimaryTrue patlar; tek primary garantisi.
        boolean newPrimaryRequested = requestList.stream().anyMatch(CarImageRequest::isPrimary);
        if (newPrimaryRequested) {
            carImageRepository.findByCarId(carId).forEach(old -> old.setPrimary(false));
        }

        boolean primaryAssigned = false;
        List<CarImage> images = new ArrayList<>();
        for (CarImageRequest req : requestList) {
            CarImage img = carImageMapper.toEntity(req);
            img.setCar(dbCar);
            if (img.isPrimary()) {
                if (primaryAssigned) {
                    img.setPrimary(false);
                } else {
                    primaryAssigned = true;
                }
            }
            images.add(img);
        }
        List<CarImage> savedCarImages = carImageRepository.saveAll(images);
        return carImageMapper.toListResponse(savedCarImages);
    }

    @Override
    @Transactional
    public void setPrimaryImage(UUID carId, UUID imageId, UUID sellerId) {
        // DÜZELTME: hata tipi IMAGE_NOT_FOUND olmalı (eskiden CAR_NOT_FOUND idi).
        CarImage dbCarImage = carImageRepository.findById(imageId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.IMAGE_NOT_FOUND, imageId.toString())));
        if (!dbCarImage.getCar().getId().equals(carId)) {
            log.error("bu görsel belirtilen araca ait değil");
            throw new BaseException(new ErrorMessage(MessageType.IMAGE_DOES_NOT_BELONG_TO_CAR, carId.toString()));
        }
        if (!dbCarImage.getCar().getSeller().getId().equals(sellerId)) {
            log.error("bu işlem için yetkiniz yok");
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION, sellerId.toString()));
        }
        carImageRepository.findByCarIdAndIsPrimaryTrue(carId).ifPresent(oldPrimary -> {
            oldPrimary.setPrimary(false);
            carImageRepository.save(oldPrimary);
        });
        dbCarImage.setPrimary(true);
        carImageRepository.save(dbCarImage);
        log.info("Car {} primary image set to {} ", carId, imageId);
    }

    @Override
    @Transactional
    public void deleteImage(UUID imageId, UUID sellerId) {
        CarImage dbCarImage = carImageRepository.findById(imageId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.IMAGE_NOT_FOUND, imageId.toString())));
        if (!dbCarImage.getCar().getSeller().getId().equals(sellerId)) {
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION, sellerId.toString()));
        }
        String url = dbCarImage.getImageUrl();
        carImageRepository.delete(dbCarImage);
        imageStorage.deleteQuietly(url);
        log.info("Image {} deleted", imageId);
    }
}
