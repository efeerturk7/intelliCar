package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.model.User;

import java.util.UUID;

public interface UserService {
    UserResponse register(RegisterRequest registerRequest);
    UserResponse getProfile(UUID id);
    User getUserEntityById(UUID id);
}
