package com.efeerturk.intelliCar.service.impl;


import com.efeerturk.intelliCar.dto.request.CarCreateRequest;
import com.efeerturk.intelliCar.dto.request.CarFilterRequest;
import com.efeerturk.intelliCar.dto.request.CarImageRequest;
import com.efeerturk.intelliCar.dto.request.CarUpdateRequest;
import com.efeerturk.intelliCar.dto.response.CarDetailResponse;
import com.efeerturk.intelliCar.dto.response.CarListResponse;
import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.mapper.CarMapper;

import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.CarImage;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.CarRepository;

import com.efeerturk.intelliCar.repository.specification.CarSpecification;
import com.efeerturk.intelliCar.service.CarService;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class CarServiceImpl implements CarService  {
    private final CarRepository carRepository;
    private final UserService userService;
    private final CarMapper carMapper;
    @Override
    @Transactional
    public CarDetailResponse createCar(CarCreateRequest carCreateRequest, UUID sellerId) {
        User seller = userService.getUserEntityById(sellerId);
        Car car=carMapper.toEntity(carCreateRequest);
            car.setSeller(seller);
            car.setStatus(CarStatus.ACTIVE);
            car.setViewCount(0);
            if (carCreateRequest.images()!=null && !carCreateRequest.images().isEmpty()) {
                car.setImages(new ArrayList<>());
                for (CarImageRequest imgReq : carCreateRequest.images()){
                    CarImage image=new CarImage();
                    image.setImageUrl(imgReq.imageUrl());
                    image.setPrimary(imgReq.isPrimary());
                    image.setCar(car);
                    car.getImages().add(image);
                }
            }
        Car savedCar=carRepository.save(car);
        log.info("Car created with id {}",savedCar.getId());
        return carMapper.toDetailResponse(savedCar);
            }
        @Override
        @Transactional(readOnly = true)
        public CarDetailResponse getCarById(UUID carId) {
          Car dbCar = carRepository.findByIdWithDetails(carId).orElseThrow(() -> new RuntimeException("Car not found with id: " + carId));

          dbCar.setViewCount(dbCar.getViewCount()+1);
          Car savedCar=carRepository.save(dbCar);
          return carMapper.toDetailResponse(savedCar);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<CarListResponse> getAllCars(CarFilterRequest carFilterRequest, Pageable pageable) {
            Specification<Car>carSpecification= CarSpecification.filterByCriteria(carFilterRequest);
            Page<Car>carPage=carRepository.findAll(carSpecification, pageable);
            return carPage.map(carMapper::toListResponse);
        }
        @Override
        @Transactional(readOnly = true)
        public Page<CarListResponse>getMyCars(UUID sellerId, Pageable pageable) {
         Page<Car>carPage=carRepository.findBySellerId(sellerId, pageable);
         return carPage.map(carMapper::toListResponse);
        }
        @Override
        @Transactional
        public CarDetailResponse updateCar(UUID carId, CarUpdateRequest carUpdateRequest,UUID currentUserId) {
            Car dbCar=carRepository.findById(carId).orElseThrow(() -> new RuntimeException("Car not found with id: " + carId));
            if (!dbCar.getSeller().getId().equals(currentUserId)){
                log.error("Current user is not the same user as the current user");
                return null;
            }
            dbCar.setPrice(carUpdateRequest.price());
            dbCar.setStatus(carUpdateRequest.status());
            dbCar.setDescription(carUpdateRequest.description());
            dbCar.setMileage(carUpdateRequest.mileage());
            Car savedCar=carRepository.save(dbCar);
            return carMapper.toDetailResponse(savedCar);
        }
        @Override
        @Transactional
        public void deleteCar(UUID carId,UUID currentUserId) {
          Car dbCar=carRepository.findById(carId).orElseThrow(() -> new RuntimeException("Car not found with id: " + carId));
          if (!dbCar.getSeller().getId().equals(currentUserId)){
              log.error("Current user is not the same user as the current user");
              throw new RuntimeException("Current user is not the same user as the current user");
          }
          dbCar.setStatus(CarStatus.INACTIVE);
          carRepository.save(dbCar);

        }



    }



