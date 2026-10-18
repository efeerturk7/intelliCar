package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.CarCreateRequest;
import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.CarImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CarImageMapper.class})
public interface CarMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "images", ignore = true)
    Car toEntity(CarCreateRequest request);

    CarDetailResponse toDetailResponse(Car car);


    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "primaryImageUrl")
    CarListResponse toListResponse(Car car);

    @Named("primaryImageUrl")
    default String primaryImageUrl(List<CarImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(CarImage::isPrimary)
                .map(CarImage::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }
}
