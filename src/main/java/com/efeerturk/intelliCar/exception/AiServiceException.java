package com.efeerturk.intelliCar.exception;

/**
 * Yapay zeka servisi yanıt vermediğinde veya geçersiz yanıt döndüğünde fırlatılır.
 * GlobalExceptionHandler bunu HTTP 503 olarak döner (BaseException'ın 400'ünden ayrı).
 */
public class AiServiceException extends BaseException {
    public AiServiceException(ErrorMessage message) {
        super(message);
    }
}
