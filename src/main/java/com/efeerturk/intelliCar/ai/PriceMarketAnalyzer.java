package com.efeerturk.intelliCar.ai;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Fiyat tahmininin SAYISAL kısmı. Spring'e bağımlı değildir, yapay zeka kullanmaz.
 *
 *  - Aynı markadaki ilanlar arasından satılacak araca en çok benzeyenleri seçer.
 *  - Benzer ilanların fiyat istatistiğini (en düşük, medyan, en yüksek, alt/üst çeyrek) hesaplar.
 *  - Modelin önerdiği fiyatın makul olup olmadığını denetler.
 *
 * Böylece fiyat aralığı her zaman gerçek ilan verilerine dayanır; Gemini sadece yıl/km/durum farklarını yorumlar.
 */
public final class PriceMarketAnalyzer {

    public static final int MAX_COMPARABLES = 15;

    private static final int MIN_SAME_MODEL_FOR_BASELINE = 3;
    private static final int MIN_FOR_QUARTILES = 4;
    // Modelin önerdiği fiyat, benzer ilanların fiyat sınırlarının bu kadar dışına çıkamaz.
    private static final double LOWER_BOUND_FACTOR = 0.6;
    private static final double UPPER_BOUND_FACTOR = 1.4;
    // En yüksek fiyat, en düşük fiyatın en fazla 2 katı olabilir (çok geniş aralık işe yaramaz).
    private static final double MAX_RANGE_RATIO = 2.0;

    private PriceMarketAnalyzer() {
    }

    /** Satılmak istenen aracın karşılaştırmada kullanılan özellikleri. */
    public record Target(String model, int year, int mileage, FuelType fuelType, Transmission transmission, String city) {
    }

    /**
     * @param comparables   araca en çok benzeyen ilanlar (en benzerden başlayarak)
     * @param sameModelCount comparables içinde aynı modelden olanların sayısı
     * @param baselineCount  istatistiğin hesaplandığı ilan sayısı
     * @param marketMin/marketMedian/marketMax istatistik ilanlarının en düşük / medyan / en yüksek fiyatı
     * @param baselineLow/baselineHigh istatistiksel fiyat aralığı (4+ ilanda alt/üst çeyrek, azında min/max)
     * @param boundsMin/boundsMax tüm benzer ilanların en düşük / en yüksek fiyatı (modeli denetlemek için)
     */
    public record Market(
            List<CarCandidate> comparables,
            int sameModelCount,
            int baselineCount,
            long marketMin,
            long marketMedian,
            long marketMax,
            long baselineLow,
            long baselineHigh,
            long boundsMin,
            long boundsMax
    ) {
    }

    public static Market analyze(Target target, List<CarCandidate> pool) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("pool must not be empty");
        }

        // sorted() kararlıdır: benzerlik eşitse havuzdaki sıra (en yeni ilan önce) korunur.
        List<CarCandidate> ranked = pool.stream()
                .sorted(Comparator.comparingDouble((CarCandidate c) -> distance(target, c)))
                .limit(MAX_COMPARABLES)
                .toList();

        List<CarCandidate> sameModel = ranked.stream()
                .filter(c -> isSameModel(target.model(), c.model()))
                .toList();

        // Yeterli sayıda aynı model varsa istatistik sadece onlardan çıkar, yoksa tüm benzer ilanlardan.
        List<CarCandidate> baseline = sameModel.size() >= MIN_SAME_MODEL_FOR_BASELINE ? sameModel : ranked;

        long[] prices = baseline.stream().mapToLong(c -> toLong(c)).sorted().toArray();
        long[] allPrices = ranked.stream().mapToLong(c -> toLong(c)).sorted().toArray();

        long median = Math.round(percentile(prices, 0.5));
        long low;
        long high;
        if (prices.length >= MIN_FOR_QUARTILES) {
            low = Math.round(percentile(prices, 0.25));
            high = Math.round(percentile(prices, 0.75));
        } else {
            low = prices[0];
            high = prices[prices.length - 1];
        }
        // Tek ilan (veya hepsi aynı fiyat) varsa sıfır genişlikli aralık yerine +-%10 kullan.
        if (low == high) {
            low = Math.round(median * 0.9);
            high = Math.round(median * 1.1);
        }

        return new Market(
                ranked,
                sameModel.size(),
                baseline.size(),
                prices[0],
                median,
                prices[prices.length - 1],
                low,
                high,
                allPrices[0],
                allPrices[allPrices.length - 1]);
    }

    /** Modelin döndürdüğü fiyat aralığı makul mü? Değilse servis istatistiksel aralığa döner. */
    public static boolean isPlausible(Long min, Long suggested, Long max, Market market) {
        if (min == null || suggested == null || max == null) {
            return false;
        }
        if (min <= 0) {
            return false;
        }
        if (min > suggested || suggested > max) {
            return false;
        }
        if (min < market.boundsMin() * LOWER_BOUND_FACTOR) {
            return false;
        }
        if (max > market.boundsMax() * UPPER_BOUND_FACTOR) {
            return false;
        }
        return max <= min * MAX_RANGE_RATIO;
    }

    /** Küçük değer = daha benzer. Aynı model, yakın yıl ve kilometre en çok ağırlığa sahip. */
    static double distance(Target t, CarCandidate c) {
        double d = 0;
        if (!isSameModel(t.model(), c.model())) {
            d += 30;
        }
        d += Math.abs(t.year() - c.year()) * 4.0;
        d += Math.min(Math.abs(t.mileage() - c.mileage()) / 10_000.0, 20);
        if (t.fuelType() != c.fuelType()) {
            d += 5;
        }
        if (t.transmission() != c.transmission()) {
            d += 3;
        }
        if (t.city() == null || c.city() == null || !t.city().trim().equalsIgnoreCase(c.city().trim())) {
            d += 1;
        }
        return d;
    }

    static boolean isSameModel(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }
        return x.equals(y) || x.contains(y) || y.contains(x);
    }

    static double percentile(long[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double pos = p * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        return sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo);
    }

    private static long toLong(CarCandidate c) {
        return c.price().setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
