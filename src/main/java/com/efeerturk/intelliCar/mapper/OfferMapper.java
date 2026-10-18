package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.model.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface OfferMapper {


    @Mapping(target = "offerId", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Offer toEntity(OfferCreateRequest request);


    @Mapping(source = "offerId", target = "id")
    @Mapping(source = "car.id", target = "carId")
    @Mapping(target = "carTitle",
            expression = "java(offer.getCar().getBrand() + \" \" + offer.getCar().getModel())")
    OfferResponse toResponse(Offer offer);
}
