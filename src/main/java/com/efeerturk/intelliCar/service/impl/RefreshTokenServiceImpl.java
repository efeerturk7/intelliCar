package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.model.RefreshToken;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.RefreshTokenRepository;
import com.efeerturk.intelliCar.repository.UserRepository;
import com.efeerturk.intelliCar.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${intellicar.security.jwt.refresh-expiration}")
    private Long refreshExpiration;
    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Override
    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(RefreshToken.builder().user(user).build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new BaseException(new ErrorMessage(MessageType.REFRESH_TOKEN_EXPIRED, token.toString()));
        }
        return token;
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.REFRESH_TOKEN_NOT_FOUND, token)));
    }
    @Transactional
    @Override
    public void deleteByUserId(UUID userId) {
        User user =  userRepository.findById(userId).orElse(null);
        refreshTokenRepository.deleteByUser(user);
    }
}
