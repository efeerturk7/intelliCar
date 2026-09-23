package com.efeerturk.intelliCar.repository;

import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CarRepository extends JpaRepository<Car, UUID>, JpaSpecificationExecutor<Car> {
    Page<Car> findByStatus(CarStatus status, Pageable pageable);

    Page<Car> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("SELECT c FROM Car c LEFT JOIN FETCH c.images LEFT JOIN FETCH c.seller WHERE c.id = :id")
    Optional<Car> findByIdWithDetails(@Param("id") UUID id);
}
