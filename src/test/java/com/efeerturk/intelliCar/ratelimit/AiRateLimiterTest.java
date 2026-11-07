package com.efeerturk.intelliCar.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker gerektirmeyen hızlı birim testi: sayaç sahte (bellekte) ve zaman elle ilerletiliyor.
 */
class AiRateLimiterTest {

    /** Redis yerine bellekte çalışan, saati elle ilerletilebilen sayaç. */
    static class FakeCounter implements WindowCounter {
        long nowMillis = 0;
        int calls = 0;
        private final Map<String, long[]> entries = new HashMap<>(); // key -> {value, expiresAtMillis}

        @Override
        public Count increment(String key, Duration window) {
            calls++;
            long[] entry = entries.get(key);
            if (entry == null || entry[1] <= nowMillis) {
                entry = new long[]{0, nowMillis + window.toMillis()};
                entries.put(key, entry);
            }
            entry[0]++;
            long remainingSeconds = Math.max(1, (entry[1] - nowMillis + 999) / 1000);
            return new Count(entry[0], remainingSeconds);
        }

        void advance(Duration duration) {
            nowMillis += duration.toMillis();
        }
    }

    private final UUID user = UUID.randomUUID();

    @Test
    void allowsRequestsUpToTheMinuteLimitThenDenies() {
        AiRateLimiter limiter = new AiRateLimiter(new FakeCounter(), 3, 0);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.check(user).allowed()).as("request %d", i + 1).isTrue();
        }
        AiRateLimiter.Decision denied = limiter.check(user);

        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void usersHaveSeparateQuotas() {
        AiRateLimiter limiter = new AiRateLimiter(new FakeCounter(), 1, 0);

        assertThat(limiter.check(user).allowed()).isTrue();
        assertThat(limiter.check(user).allowed()).isFalse();
        assertThat(limiter.check(UUID.randomUUID()).allowed()).isTrue();
    }

    @Test
    void allowsAgainAfterTheWindowPasses() {
        FakeCounter counter = new FakeCounter();
        AiRateLimiter limiter = new AiRateLimiter(counter, 3, 0);
        for (int i = 0; i < 4; i++) {
            limiter.check(user);
        }

        counter.advance(Duration.ofSeconds(61));

        assertThat(limiter.check(user).allowed()).isTrue();
    }

    @Test
    void dailyLimitDeniesWithLongRetryAfter() {
        FakeCounter counter = new FakeCounter();
        AiRateLimiter limiter = new AiRateLimiter(counter, 3, 5);

        int allowed = 0;
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 4; i++) {
                if (limiter.check(user).allowed()) {
                    allowed++;
                }
            }
            counter.advance(Duration.ofSeconds(61)); // dakika penceresi sıfırlanır, gün penceresi sürer
        }

        assertThat(allowed).isEqualTo(5);
        AiRateLimiter.Decision denied = limiter.check(user);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isGreaterThan(3600L);
    }

    @Test
    void requestDeniedByMinuteLimitDoesNotConsumeDailyQuota() {
        FakeCounter counter = new FakeCounter();
        AiRateLimiter limiter = new AiRateLimiter(counter, 3, 5);

        // 3 izinli + 10 reddedilen istek: günlük sayaç sadece 3 artmalı
        for (int i = 0; i < 13; i++) {
            limiter.check(user);
        }
        counter.advance(Duration.ofSeconds(61));

        int allowed = 0;
        for (int i = 0; i < 5; i++) {
            if (limiter.check(user).allowed()) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(2); // günlük 5 - 3 kullanılan = 2
    }

    @Test
    void zeroLimitDisablesTheWindowAndNeverTouchesTheCounter() {
        FakeCounter counter = new FakeCounter();
        AiRateLimiter limiter = new AiRateLimiter(counter, 0, 0);

        for (int i = 0; i < 100; i++) {
            assertThat(limiter.check(user).allowed()).isTrue();
        }
        assertThat(counter.calls).isZero();
    }
}
