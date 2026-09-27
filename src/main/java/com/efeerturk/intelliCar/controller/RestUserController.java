package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.response.UserResponse;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/intelliCar/api/v1/users")
@RequiredArgsConstructor
public class RestUserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(@AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserProfileById(@PathVariable UUID id) {
        UserResponse response = userService.getProfile(id);
        return ResponseEntity.ok(response);
    }
}
