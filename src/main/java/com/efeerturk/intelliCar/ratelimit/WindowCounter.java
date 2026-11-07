package com.efeerturk.intelliCar.ratelimit;

import java.time.Duration;

/**
 * Sabit zaman penceresi sayacı. Üretimde Redis kullanır (RedisWindowCounter),
 * bu arayüz sayesinde limit mantığı Redis olmadan da test edilebilir.
 */
public interface WindowCounter {

    /**
     * key için sayacı bir artırır. Pencerenin ilk isteğinde sayaç, window süresi sonunda silinecek şekilde oluşturulur.
     */
    Count increment(String key, Duration window);

    /**
     * @param value            pencere içindeki istek sayısı (bu istek dahil)
     * @param remainingSeconds pencerenin bitmesine kalan süre (en az 1)
     */
    record Count(long value, long remainingSeconds) {
    }
}
