package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.CarImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring",uses = CarMapper.class)
public interface CarImageMapper {
    CarImage toEntity(CarImageRequest request);
    CarImageResponse toDetailResponse(CarImage carImage);
    List<CarImageResponse> toListResponse(List<CarImage> carImages);
}
