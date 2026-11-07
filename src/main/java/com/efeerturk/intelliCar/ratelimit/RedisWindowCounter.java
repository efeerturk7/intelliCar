package com.efeerturk.intelliCar.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis INCR + EXPIRE ile sabit pencere sayacı. Sayaçlar Redis'te durduğu için
 * uygulamanın birden fazla kopyası çalışsa bile limit ortaktır.
 */
@Component
@RequiredArgsConstructor
public class RedisWindowCounter implements WindowCounter {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Count increment(String key, Duration window) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value == null) {
            throw new IllegalStateException("Redis returned no value for INCR");
        }

        long remaining;
        if (value == 1) {
            // Pencerenin ilk isteği: süreyi başlat.
            redisTemplate.expire(key, window);
            remaining = window.toSeconds();
        } else {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                // INCR ile EXPIRE arasında uygulama çökerse anahtar süresiz kalır ve kullanıcı sonsuza dek engellenir.
                // Süresi olmayan anahtarı burada yeniden başlatarak bunu önlüyoruz.
                redisTemplate.expire(key, window);
                remaining = window.toSeconds();
            } else {
                remaining = ttl;
            }
        }
        return new Count(value, Math.max(remaining, 1));
    }
}
