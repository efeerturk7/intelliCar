package com.efeerturk.intelliCar.config;

import com.efeerturk.intelliCar.ratelimit.AiRateLimiter;
import com.efeerturk.intelliCar.ratelimit.WindowCounter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public AiRateLimiter aiRateLimiter(
            WindowCounter windowCounter,
            @Value("${intellicar.ai.rate-limit.per-minute:5}") int perMinute,
            @Value("${intellicar.ai.rate-limit.per-day:50}") int perDay
    ) {
        return new AiRateLimiter(windowCounter, perMinute, perDay);
    }
}
