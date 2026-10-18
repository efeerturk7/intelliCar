package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.CarCreateRequest;
import com.efeerturk.intelliCar.dto.request.CarFilterRequest;
import com.efeerturk.intelliCar.dto.request.CarUpdateRequest;
import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import com.efeerturk.intelliCar.event.CarViewedEvent;
import com.efeerturk.intelliCar.kafka.KafkaEventPublisher;
import com.efeerturk.intelliCar.kafka.KafkaTopics;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/car")
public class RestCarController {

    private final CarService carService;
    private final KafkaEventPublisher kafkaEventPublisher;


    @PostMapping
    public ResponseEntity<CarDetailResponse> createCar(
            @Valid @RequestBody CarCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        CarDetailResponse response = carService.createCar(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping("/{id}")
    public ResponseEntity<CarDetailResponse> getCarById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        CarDetailResponse response = carService.getCarById(id);
        kafkaEventPublisher.publish(KafkaTopics.CAR_VIEW_EVENTS, id.toString(),
                new CarViewedEvent(id, currentUser != null ? currentUser.getId() : null, Instant.now()));
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<Page<CarListResponse>> getAllCars(
            @ModelAttribute CarFilterRequest filterRequest,
            Pageable pageable
    ) {
        return ResponseEntity.ok(carService.getAllCars(filterRequest, pageable));
    }


    @GetMapping("/my-cars")
    public ResponseEntity<Page<CarListResponse>> getMyCars(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable
    ) {
        return ResponseEntity.ok(carService.getMyCars(currentUser.getId(), pageable));
    }


    @PutMapping("/{id}")
    public ResponseEntity<CarDetailResponse> updateCar(
            @PathVariable UUID id,
            @Valid @RequestBody CarUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(carService.updateCar(id, request, currentUser.getId()));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        carService.deleteCar(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
