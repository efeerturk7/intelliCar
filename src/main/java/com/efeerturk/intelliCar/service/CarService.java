package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.CarCreateRequest;
import com.efeerturk.intelliCar.dto.request.CarFilterRequest;
import com.efeerturk.intelliCar.dto.request.CarUpdateRequest;
import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CarService {
    CarDetailResponse createCar(CarCreateRequest carCreateRequest, UUID sellerId);
    CarDetailResponse getCarById(UUID carId);
    Page<CarListResponse> getAllCars(CarFilterRequest carFilterRequest, Pageable pageable);
    Page<CarListResponse>getMyCars(UUID sellerId, Pageable pageable);
    CarDetailResponse updateCar(UUID carId, CarUpdateRequest carUpdateRequest, UUID currentUserId);
    void deleteCar(UUID carId,UUID currentUserId);

}
