package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.LoginRequest;
import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.AuthResponse;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.model.User;

import java.util.UUID;

public interface UserService {
    AuthResponse register(RegisterRequest registerRequest);
    UserResponse getProfile(UUID id);
    User getUserEntityById(UUID id);
    AuthResponse login(LoginRequest loginRequest);
}
