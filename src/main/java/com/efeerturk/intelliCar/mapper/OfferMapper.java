package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.OfferCreateRequest;
import com.efeerturk.intelliCar.dto.response.OfferResponse;
import com.efeerturk.intelliCar.model.Offer;
import org.mapstruct.Mapper;

@Mapper(componentModel ="spring",uses = {UserMapper.class,CarMapper.class})
public interface OfferMapper {
    Offer toEntity(OfferCreateRequest request);
    OfferResponse toResponse(Offer offer);
}
