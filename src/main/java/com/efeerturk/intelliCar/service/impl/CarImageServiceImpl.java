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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CarImageServiceImpl implements CarImageService {
    private final CarImageRepository carImageRepository;
    private final CarRepository carRepository;
    private CarImageMapper carImageMapper;
    @Override
    @Transactional
    public List<CarImageResponse>addImagesToCar(UUID carId, List<CarImageRequest> requestList, UUID sellerId){
        Car dbCar=carRepository.findById(carId).orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.CAR_NOT_FOUND,carId.toString())));
        if (!dbCar.getSeller().getId().equals(sellerId)){
            log.error("Bu ilana görsel ekleme yetkiniz yok");
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION,sellerId.toString()));
        }
        List<CarImage> images = requestList.stream().map(req -> {
            CarImage img = carImageMapper.toEntity(req);
            img.setCar(dbCar);
            return img;
        }).toList();
        List<CarImage>savedCarImages=carImageRepository.saveAll(images);
        return carImageMapper.toListResponse(savedCarImages);
    }
    @Override
    @Transactional
    public void setPrimaryImage(UUID carId,UUID imageId,UUID sellerId){
        CarImage dbCarImage=carImageRepository.findById(imageId).orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.CAR_NOT_FOUND,carId.toString())));
        if (!dbCarImage.getCar().getId().equals(carId)){
            log.error("bu görsel belirtilen araca ait değil");
            throw new BaseException(new ErrorMessage(MessageType.IMAGE_DOES_NOT_BELONG_TO_CAR,dbCarImage.getCar().getId().toString()));
        }
        if (!dbCarImage.getCar().getSeller().getId().equals(sellerId)){
            log.error("bu işlem için yetkiniz yok");
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION,sellerId.toString()));
        }
        carImageRepository.findByCarIdAndIsPrimaryTrue(carId).ifPresent(oldPrimary -> {
            oldPrimary.setPrimary(false);
            carImageRepository.save(oldPrimary);
        });
        dbCarImage.setPrimary(true);
        carImageRepository.save(dbCarImage);
        log.info("Car {} primary image set to {} ",carId,imageId);
    }
    @Override
    @Transactional
    public void deleteImage(UUID imageId,UUID sellerId){
        CarImage dbCarImage=carImageRepository.findById(imageId).orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        if (!dbCarImage.getCar().getSeller().getId().equals(sellerId)){
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED_IMAGE_OPERATION,sellerId.toString()));
        }
        carImageRepository.delete(dbCarImage);
        log.info("Car {} primary image deleted",imageId);
    }
}
