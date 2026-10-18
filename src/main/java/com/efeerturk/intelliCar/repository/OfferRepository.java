package com.efeerturk.intelliCar.repository;

import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.model.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    Page<Offer> findByCarId(UUID carId, Pageable pageable);

    Page<Offer> findByBuyerId(UUID buyerId, Pageable pageable);

    boolean existsByCarIdAndBuyerIdAndStatus(UUID carId, UUID buyerId, OfferStatus status);

    // KAFKA: CarStatusConsumer tarafından kullanılır. Etkilenen teklif sayısını döner.
    @Modifying
    @Query("UPDATE Offer o SET o.status = :newStatus WHERE o.car.id = :carId AND o.status = :oldStatus")
    int expirePendingOffers(@Param("carId") UUID carId,
                            @Param("newStatus") OfferStatus newStatus,
                            @Param("oldStatus") OfferStatus oldStatus);
}
