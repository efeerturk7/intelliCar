package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.CarCreateRequest;

import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import com.efeerturk.intelliCar.model.Car;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {UserMapper.class, CarImageMapper.class})
public interface CarMapper {
    Car toEntity(CarCreateRequest request);
    CarDetailResponse toDetailResponse(Car car);
    CarListResponse toListResponse(Car car);

}
