package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.service.OfferService;
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
@RequestMapping("/intelliCar/api/v1/offer")
public class RestOfferController {
    private final OfferService offerService;
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody OfferCreateRequest request,@RequestHeader("X-User-Id") UUID buyerId){
        OfferResponse response=offerService.createOffer(request,buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PatchMapping("/{offerId}/respond")
    public ResponseEntity<OfferResponse> respondToOffer(@PathVariable UUID offerId, @RequestParam OfferStatus newStatus,@RequestHeader("X-User-Id") UUID sellerId){
        OfferResponse response=offerService.respondToOffer(offerId,newStatus,sellerId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/cars/{carId}")
    public ResponseEntity<Page<OfferResponse>> getOffersForCar(@PathVariable UUID carId,@RequestHeader("X-User-Id") UUID sellerId, Pageable pageable){
        Page<OfferResponse> response=offerService.getOffersForCar(carId,sellerId,pageable);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/get-my-offers")
    public ResponseEntity<Page<OfferResponse>>getMyOffers(@RequestHeader("X-User-Id")UUID buyerId,Pageable pageable){
        Page<OfferResponse> response=offerService.getMyOffers(buyerId,pageable);
        return ResponseEntity.ok(response);

    }
}
