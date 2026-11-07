package com.efeerturk.intelliCar.exception;

/**
 * İstek limiti aşıldığında fırlatılır. GlobalExceptionHandler bunu HTTP 429 ve Retry-After başlığı olarak döner.
 */
public class RateLimitExceededException extends BaseException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(ErrorMessage message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
