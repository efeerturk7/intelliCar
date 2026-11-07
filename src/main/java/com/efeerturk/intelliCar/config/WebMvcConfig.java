package com.efeerturk.intelliCar.config;

import com.efeerturk.intelliCar.ratelimit.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${intellicar.ai.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/intelliCar/api/v1/ai/**");
        }
    }
}
