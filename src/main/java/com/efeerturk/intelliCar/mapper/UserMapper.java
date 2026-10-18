package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.SellerSummaryResponse;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    UserResponse toResponse(User user);

    SellerSummaryResponse toSellerSummary(User user);
}
