package com.efeerturk.intelliCar.repository;

import com.efeerturk.intelliCar.model.CarImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarImageRepository extends JpaRepository<CarImage, UUID> {
    List<CarImage> findByCarId(UUID carId);
    Optional<CarImage>findByCarIdAndIsPrimaryTrue(UUID carId);
}
