package com.efeerturk.intelliCar.enums;

/**
 * Kullanıcı tek tiptir: kayıt olan herkes hem ilan verebilir hem teklif verebilir (USER).
 * ADMIN sadece yönetim içindir ve kayıt sırasında istenemez.
 * BUYER ve SELLER, eski kayıtların veritabanında okunabilmesi için durur, yeni kullanıcılara verilmez.
 */
public enum UserRole {
    USER,
    ADMIN,
    @Deprecated BUYER,
    @Deprecated SELLER
}
