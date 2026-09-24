package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OfferService  {
    Page<OfferResponse> getMyOffers(UUID buyerId, Pageable pageable);
    Page<OfferResponse> getOffersForCar(UUID carId, UUID sellerId, Pageable pageable);
    OfferResponse respondToOffer(UUID offerId, OfferStatus newStatus, UUID sellerId);
    OfferResponse createOffer(OfferCreateRequest request, UUID buyerId);
}
