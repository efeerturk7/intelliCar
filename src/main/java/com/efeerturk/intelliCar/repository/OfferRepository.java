package com.efeerturk.intelliCar.repository;

import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    Page<Offer> findByCarId(UUID carId, Pageable pageable);

    Page<Offer> findByBuyerId(UUID buyerId, Pageable pageable);

    boolean existsByCarIdAndBuyerIdAndStatus(UUID carId, UUID buyerId, OfferStatus status);
}
