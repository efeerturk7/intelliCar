package com.efeerturk.intelliCar.ai;

/** Fiyatın başlangıç noktasının (baz fiyat) nasıl hesaplandığı. */
public enum PriceMethod {
    /** Aynı marka, model ve yıldaki en az 2 ilanın kilometreye göre karşılaştırılması (en güvenilir yöntem). */
    MILEAGE_BASED,
    /** Aynı marka, model ve yıldan yalnızca 1 ilan var. */
    SINGLE_COMPARABLE,
    /** Aynı model ve yıldan ilan yok: aynı markadaki en benzer ilanların fiyat dağılımı. */
    SIMILAR_LISTINGS
}
