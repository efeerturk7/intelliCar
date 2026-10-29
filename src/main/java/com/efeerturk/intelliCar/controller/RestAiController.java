package com.efeerturk.intelliCar.controller;

import com.efeerturk.intelliCar.dto.request.BudgetRecommendationRequest;
import com.efeerturk.intelliCar.dto.response.BudgetRecommendationResponse;
import com.efeerturk.intelliCar.model.User;
import com.efeerturk.intelliCar.service.AiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/intelliCar/api/v1/ai")
public class RestAiController {

    private final AiRecommendationService aiRecommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<BudgetRecommendationResponse> recommendByBudget(
            @Valid @RequestBody BudgetRecommendationRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(aiRecommendationService.recommendByBudget(request, currentUser.getId()));
    }
}
