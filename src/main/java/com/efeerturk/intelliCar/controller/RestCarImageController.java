package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.response.CarImageResponse;
import com.efeerturk.intelliCar.service.CarImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/carImage")
public class RestCarImageController {
    private final CarImageService carImageService;
    public List<CarImageResponse> addImagesToCar(@Valid UUID carId, List<CarImageRequest> requestList, UUID sellerId){

    }
}
