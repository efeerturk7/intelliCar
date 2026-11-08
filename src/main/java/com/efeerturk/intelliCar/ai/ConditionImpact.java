package com.efeerturk.intelliCar.ai;

import java.util.Locale;

/**
 * Satılacak aracın durumunun (hasar kaydı, değişen, boya) benzer ilanlara göre fiyata etkisi.
 * Gemini bunu araç ve ilan açıklamalarını okuyarak belirler; kod, fiyatın bu yönle tutarlı olup olmadığını denetler.
 */
public enum ConditionImpact {
    /** Araç benzerlerinden daha iyi durumda (örn. boyasız, değişensiz, hasar kaydı yok): fiyat artmalı. */
    POSITIVE,
    /** Araç benzerleriyle aynı durumda. */
    NEUTRAL,
    /** Araç benzerlerinden daha kötü durumda (örn. hasar kaydı, boyalı veya değişen parça): fiyat düşmeli. */
    NEGATIVE,
    /** Açıklamada durumla ilgili bilgi yok. */
    UNKNOWN;

    /** Modelin metin olarak döndürdüğü değeri okur. Tanınmayan veya boş değer UNKNOWN sayılır (hata fırlatmaz). */
    public static ConditionImpact parse(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
