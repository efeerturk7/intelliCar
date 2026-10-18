package com.efeerturk.intelliCar.event;

import com.efeerturk.intelliCar.enums.CarStatus;

import java.time.Instant;
import java.util.UUID;

public record CarStatusChangedEvent(UUID carId, CarStatus oldStatus, CarStatus newStatus, Instant occurredAt) {
}
