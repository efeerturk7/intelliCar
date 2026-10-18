package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.model.CarImage;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface CarImageMapper {

    default CarImage toEntity(CarImageRequest request) {
        if (request == null) {
            return null;
        }
        CarImage image = new CarImage();
        image.setImageUrl(request.imageUrl());
        image.setPrimary(request.isPrimary());
        return image;
    }

    default CarImageResponse toDetailResponse(CarImage carImage) {
        if (carImage == null) {
            return null;
        }
        return new CarImageResponse(carImage.getId(), carImage.getImageUrl(), carImage.isPrimary());
    }

    default List<CarImageResponse> toListResponse(List<CarImage> carImages) {
        if (carImages == null) {
            return List.of();
        }
        return carImages.stream().map(this::toDetailResponse).toList();
    }
}
