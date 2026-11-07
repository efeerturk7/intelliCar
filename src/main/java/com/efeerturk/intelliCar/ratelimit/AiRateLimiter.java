package com.efeerturk.intelliCar.ratelimit;

import java.time.Duration;
import java.util.UUID;

/**
 * AI endpoint'leri için kullanıcı başına limit: dakikada N ve 24 saatte M istek.
 * İki endpoint de (öneri ve fiyat tahmini) aynı kotayı paylaşır, çünkü maliyet aynı model kotasından düşer.
 *
 * Spring'e bağımlı değildir (bean olarak RateLimitConfig oluşturur), bu yüzden tek başına test edilebilir.
 * Bir limitin değeri 0 veya negatifse o pencere kapalıdır.
 */
public class AiRateLimiter {

    /** @param retryAfterSeconds izin verilmediyse, tekrar denemek için beklenmesi gereken süre */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static Decision allow() {
            return new Decision(true, 0);
        }
    }

    private final WindowCounter counter;
    private final int perMinute;
    private final int perDay;

    public AiRateLimiter(WindowCounter counter, int perMinute, int perDay) {
        this.counter = counter;
        this.perMinute = perMinute;
        this.perDay = perDay;
    }

    public Decision check(UUID userId) {
        Decision minute = checkWindow("ratelimit:ai:" + userId + ":minute", Duration.ofMinutes(1), perMinute);
        if (!minute.allowed()) {
            // Dakika limitine takılan istek günlük sayacı tüketmez.
            return minute;
        }
        return checkWindow("ratelimit:ai:" + userId + ":day", Duration.ofDays(1), perDay);
    }

    private Decision checkWindow(String key, Duration window, int limit) {
        if (limit <= 0) {
            return Decision.allow();
        }
        WindowCounter.Count count = counter.increment(key, window);
        if (count.value() > limit) {
            return new Decision(false, count.remainingSeconds());
        }
        return Decision.allow();
    }
}
