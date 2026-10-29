package com.efeerturk.intelliCar.service;

import com.efeerturk.intelliCar.dto.request.BudgetRecommendationRequest;
import com.efeerturk.intelliCar.dto.response.BudgetRecommendationResponse;

import java.util.UUID;

public interface AiRecommendationService {

    /**
     * Kullanıcının bütçesine ve tercihlerine göre, veritabanındaki gerçek aktif ilanlar arasından
     * en iyi araçları yapay zeka ile sıralar.
     */
    BudgetRecommendationResponse recommendByBudget(BudgetRecommendationRequest request, UUID currentUserId);
}
