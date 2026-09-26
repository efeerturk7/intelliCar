package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/offer")
public class RestOfferController {

    private final OfferService offerService;


    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody OfferCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        OfferResponse response = offerService.createOffer(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping("/{id}/respond")
    public ResponseEntity<OfferResponse> respondToOffer(
            @PathVariable UUID id,
            @RequestParam OfferStatus newStatus,
            @AuthenticationPrincipal User currentUser
    ) {
        OfferResponse response = offerService.respondToOffer(id, newStatus, currentUser.getId());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/cars/{carId}")
    public ResponseEntity<Page<OfferResponse>> getOffersForCar(
            @PathVariable UUID carId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable
    ) {
        Page<OfferResponse> response = offerService.getOffersForCar(carId, currentUser.getId(), pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/my-offers")
    public ResponseEntity<Page<OfferResponse>> getMyOffers(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable
    ) {
        Page<OfferResponse> response = offerService.getMyOffers(currentUser.getId(), pageable);
        return ResponseEntity.ok(response);
    }
}
