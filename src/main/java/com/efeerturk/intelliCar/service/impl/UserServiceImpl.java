package com.efeerturk.intelliCar.service.impl;

import com.efeerturk.intelliCar.dto.request.RegisterRequest;
import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.BaseException;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.mapper.UserMapper;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.repository.UserRepository;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Override
    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.email())){
            throw new BaseException(new ErrorMessage(MessageType.THE_ACCOUNT_ALREADY_EXISTS,registerRequest.email()));
        }else {
            User user=userMapper.toEntity(registerRequest);

            User savedUser=userRepository.save(user);
            log.info("User {} has been registered successfully", savedUser.getId());
            return userMapper.toResponse(savedUser);
        }
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
