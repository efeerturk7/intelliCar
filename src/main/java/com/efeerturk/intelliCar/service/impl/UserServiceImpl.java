package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.dto.request.LoginRequest;
import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.AuthResponse;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.jwt.JwtService;
import com.efeerturk.intelliCar.mapper.UserMapper;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.UserRepository;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            log.warn("Kayıt başarısız: Email zaten kullanımda. Email: {}", registerRequest.email());
            throw new BaseException(new ErrorMessage(MessageType.THE_ACCOUNT_ALREADY_EXISTS,registerRequest.email()));
        }

        User user = userMapper.toEntity(registerRequest);

        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        User savedUser = userRepository.save(user);

        String jwtToken = jwtService.generateToken(savedUser);
        log.info("User {} registered and token generated", savedUser.getId());

        return new AuthResponse(jwtToken, savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
        } catch (Exception ex) {
            log.warn("Hatalı giriş denemesi: {}", loginRequest.email());
            throw new BaseException(new ErrorMessage(MessageType.WRONG_CREDENTIALS, loginRequest.email()));
        }

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.USER_NOT_FOUND, loginRequest.email())));

        String jwtToken = jwtService.generateToken(user);
        log.info("User {} logged in successfully", user.getId());

        return new AuthResponse(jwtToken, user.getId(), user.getEmail(), user.getRole().name());
    }
    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID id) {
            User user = getUserEntityById(id);
            log.info("User {} profile retrieved successfully", user.getId());
            return userMapper.toResponse(user);

    }
    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.USER_NOT_FOUND,id.toString())));

    }

}
