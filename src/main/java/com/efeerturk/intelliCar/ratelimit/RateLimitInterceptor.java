package com.efeerturk.intelliCar.ratelimit;

import com.efeerturk.intelliCar.enums.MessageType;
import com.efeerturk.intelliCar.exception.ErrorMessage;
import com.efeerturk.intelliCar.exception.RateLimitExceededException;
import com.efeerturk.intelliCar.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * AI endpoint'lerine gelen istekleri kullanıcı başına sınırlar (WebMvcConfig yalnızca /ai/** için kaydeder).
 *
 * Not: Interceptor, istek gövdesi doğrulanmadan önce çalışır; bu yüzden geçersiz (400 dönen) istekler de limite sayılır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final AiRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            // Giriş yapmamış istekleri Spring Security zaten reddeder; burada sınırlanacak kullanıcı yok.
            return true;
        }

        AiRateLimiter.Decision decision;
        try {
            decision = rateLimiter.check(user.getId());
        } catch (RuntimeException ex) {
            // Redis'e ulaşılamıyorsa AI endpoint'lerini tamamen kapatmak yerine isteğe izin verilir (fail-open).
            log.warn("Rate limiter kullanılamıyor (Redis?), istek limitsiz geçirildi", ex);
            return true;
        }

        if (!decision.allowed()) {
            log.warn("AI istek limiti aşıldı. userId={}, retryAfter={}s", user.getId(), decision.retryAfterSeconds());
            throw new RateLimitExceededException(
                    new ErrorMessage(MessageType.RATE_LIMIT_EXCEEDED, "try again in " + decision.retryAfterSeconds() + " seconds"),
                    decision.retryAfterSeconds());
        }
        return true;
    }
}
