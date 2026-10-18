package com.efeerturk.intelliCar.event;

import java.time.Instant;
import java.util.UUID;


public record CarViewedEvent(UUID carId, UUID viewerId, Instant occurredAt) {
}
