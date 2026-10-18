package com.efeerturk.intelliCar.event;

import com.efeerturk.intelliCar.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferEvent(
        OfferEventType eventType,
        UUID offerId,
        UUID carId,
        String carTitle,
        UUID sellerId,
        UUID buyerId,
        BigDecimal offeredPrice,
        OfferStatus status,
        Instant occurredAt
) {
    public enum OfferEventType {
        OFFER_CREATED,
        OFFER_RESPONDED
    }
}
