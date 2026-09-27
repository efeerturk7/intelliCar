package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.model.RefreshToken;
import com.efeerturk.intelliCar.model.User;

import java.util.UUID;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    RefreshToken findByToken(String token);
    void deleteByUserId(UUID userId);
}
