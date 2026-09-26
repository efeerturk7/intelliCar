package com.efeerturk.intelliCar.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageType {
    // --- AUTH & USER (1000 - 1009) ---
    THE_ACCOUNT_ALREADY_EXISTS("a1001", "Account already exists"),
    USER_NOT_FOUND("a1002", "User not found"),

    // --- CAR (1010 - 1019) ---
    CAR_NOT_FOUND("a1010", "Car not found"),
    UNAUTHORIZED_CAR_OPERATION("a1011", "You are not authorized to perform operations on this car"),
    CAR_NOT_ACTIVE("a1012", "Offers can only be made on active car listings"),

    // --- CAR IMAGE (1020 - 1029) ---
    IMAGE_NOT_FOUND("a1020", "Car image not found"),
    UNAUTHORIZED_IMAGE_OPERATION("a1021", "You are not authorized to modify images for this car"),
    IMAGE_DOES_NOT_BELONG_TO_CAR("a1022", "The specified image does not belong to this car"),

    // --- OFFER (1030 - 1039) ---
    OFFER_NOT_FOUND("a1030", "Offer not found"),
    CANNOT_OFFER_ON_OWN_CAR("a1031", "You cannot make an offer on your own vehicle"),
    PENDING_OFFER_ALREADY_EXISTS("a1032", "You already have a pending offer for this vehicle"),
    UNAUTHORIZED_OFFER_OPERATION("a1033", "You are not authorized to respond to this offer"),
    OFFER_NOT_PENDING("a1034", "Only pending offers can be responded to"),
    INVALID_OFFER_STATUS("a1035", "Invalid offer status update"),
    ONLY_THE_VEHICLE_OWNER_CAN_VIEW_THE_OFFERS("a1036", "Only the vehicle owner can view offers for this car"),

    // --- GENERAL / SYSTEM (9000+) ---
    INTERNAL_SERVER_ERROR("a9999", "An unexpected error occurred"),
    WRONG_CREDENTIALS("a1003", "Invalid email or password"),
    INVALID_TOKEN("a1004", "JWT token is invalid or expired");

    private String code;
    private String message;
}
