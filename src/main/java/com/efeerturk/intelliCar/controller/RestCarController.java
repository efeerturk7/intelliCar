package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.CarCreateRequest;
import com.efeerturk.intelliCar.dto.request.CarFilterRequest;
import com.efeerturk.intelliCar.dto.request.CarUpdateRequest;
import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import com.efeerturk.intelliCar.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/car")
public class RestCarController {
    private final CarService carService;
    @PostMapping("/create")
    public ResponseEntity<CarDetailResponse> createCar(@Valid @RequestBody CarCreateRequest carCreateRequest, @RequestHeader("X-User-Id") UUID sellerId){
        CarDetailResponse response= carService.createCar(carCreateRequest, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("/get/{id}")
    public ResponseEntity<CarDetailResponse>getCarById(@PathVariable UUID id){
        CarDetailResponse response= carService.getCarById(id);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/getAllCars")
    public ResponseEntity<Page<CarListResponse>> getAllCars(@Valid @RequestBody CarFilterRequest carFilterRequest, Pageable pageable){
        Page<CarListResponse> responsePage = carService.getAllCars(carFilterRequest, pageable);
        return  ResponseEntity.ok(responsePage);
    }
    @GetMapping("/getMyCars")
    public ResponseEntity<Page<CarListResponse>>getMyCars(@RequestHeader("X-User-Id")UUID sellerId, Pageable pageable){
        Page<CarListResponse> responsePage = carService.getMyCars(sellerId, pageable);
        return  ResponseEntity.ok(responsePage);
    }
    @PostMapping("update")
    public ResponseEntity<CarDetailResponse> updateCar(@PathVariable UUID carId, @Valid @RequestBody CarUpdateRequest carUpdateRequest,@RequestHeader("X-User-Id") UUID currentUserId){
        CarDetailResponse response= carService.updateCar(carId, carUpdateRequest, currentUserId);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("delete")
    public void deleteCar(@PathVariable UUID carId,@RequestHeader("X-User-Id")UUID currentUserId){
        carService.deleteCar(carId,currentUserId);

    }


}
