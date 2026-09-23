package com.efeerturk.intelliCar.mapper;

import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.SellerSummaryResponse;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest request);
    UserResponse toResponse(User user);
    SellerSummaryResponse toSellerSummary(User user);

}
