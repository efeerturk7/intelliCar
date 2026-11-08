package com.efeerturk.intelliCar.ai;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Fiyat tahmininin SAYISAL kısmı. Spring'e bağımlı değildir, yapay zeka kullanmaz.
 *
 * Baz fiyat üç yoldan biriyle hesaplanır (PriceMethod):
 *  1) MILEAGE_BASED: aynı marka, model VE yıldan en az 2 ilan varsa, fiyat bu ilanların kilometreye göre
 *     karşılaştırılmasıyla bulunur. Örnek: 100.000 km'lik ilan 3.000.000 TL, 150.000 km'lik ilan 2.700.000 TL ise
 *     134.000 km'lik araç için fiyat bu ikisinin arasında, kilometreyle orantılı olarak hesaplanır.
 *     Yalnızca kilometre bakımından en yakın iki ilan kullanılır, uçtaki (örn. 300.000 km) ilanlar sonucu bozmaz.
 *  2) SINGLE_COMPARABLE: aynı model ve yıldan tek ilan varsa onun fiyatı baz alınır.
 *  3) SIMILAR_LISTINGS: aynı model ve yıldan ilan yoksa aynı markanın en benzer ilanlarının fiyat dağılımı kullanılır.
 *
 * Gemini bu baz fiyatı yalnızca aracın durumuna (hasar kaydı, değişen, boya) göre ayarlar. Önerisi decide() ile denetlenir;
 * tutarsızsa baz fiyat kullanılır.
 */
public final class PriceMarketAnalyzer {

    public static final int MAX_COMPARABLES = 15;

    private static final int MIN_SAME_MODEL_FOR_BASELINE = 3;
    private static final int MIN_FOR_QUARTILES = 4;

    // Tek ilan veya benzer ilanlar yöntemi: model fiyatı, ilanların fiyat sınırlarının bu kadar dışına çıkamaz.
    private static final double LOWER_BOUND_FACTOR = 0.6;
    private static final double UPPER_BOUND_FACTOR = 1.4;
    // En yüksek fiyat, en düşük fiyatın en fazla 2 katı olabilir (çok geniş aralık işe yaramaz).
    private static final double MAX_RANGE_RATIO = 2.0;

    // Kilometre aralığının dışındaki araçlar için: baz fiyat, uçtaki ilanın fiyatından en fazla bu kadar sapabilir.
    private static final double EXTRAPOLATE_MAX_GAIN = 1.15;
    private static final double EXTRAPOLATE_MAX_LOSS = 0.85;

    // Baz fiyatın etrafındaki istatistiksel aralık genişliği (yöntemin güvenilirliğine göre)
    private static final double BAND_INTERPOLATED = 0.05;
    private static final double BAND_EXTRAPOLATED = 0.08;
    private static final double BAND_SINGLE = 0.10;

    // MILEAGE_BASED: Gemini'nin baz fiyattan ne kadar uzaklaşabileceği (kilometre ve yıl zaten baz fiyatta var)
    private static final double ANCHORED_SUGGESTED_MIN = 0.65;
    private static final double ANCHORED_SUGGESTED_MAX = 1.10;
    private static final double ANCHORED_RANGE_MIN = 0.55;
    private static final double ANCHORED_RANGE_MAX = 1.20;
    private static final double ANCHORED_MAX_RANGE_RATIO = 1.6;
    // Durum bilgisi yoksa veya benzerleriyle aynıysa fiyat baz fiyata yakın kalmalı
    private static final double NEUTRAL_TOLERANCE = 0.10;
    // Yön kontrolünde küçük bir pay (yuvarlama) tanınır
    private static final double DIRECTION_TOLERANCE = 0.02;

    private PriceMarketAnalyzer() {
    }

    /** Satılmak istenen aracın karşılaştırmada kullanılan özellikleri. */
    public record Target(String model, int year, int mileage, FuelType fuelType, Transmission transmission, String city) {
    }

    public enum Level {
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * @param comparables    modele gösterilecek ilanlar: önce aynı model ve yıldakiler, sonra en benzer diğerleri
     * @param sameModelCount comparables içinde aynı (benzer yazımlı) modelden olanların sayısı
     * @param baselineCount  istatistiğin hesaplandığı ilan sayısı
     * @param marketMin      baz fiyatın dayandığı ilanların en düşük fiyatı
     * @param marketMedian   baz fiyatın dayandığı ilanların medyan fiyatı
     * @param marketMax      baz fiyatın dayandığı ilanların en yüksek fiyatı
     * @param baselineLow    baz fiyatın etrafındaki istatistiksel aralığın alt sınırı
     * @param baselineHigh   baz fiyatın etrafındaki istatistiksel aralığın üst sınırı
     * @param boundsMin      comparables içindeki en düşük fiyat (SINGLE ve SIMILAR yöntemlerinde modeli denetlemek için)
     * @param boundsMax      comparables içindeki en yüksek fiyat
     * @param method         baz fiyatın hesaplanma yöntemi
     * @param baselinePrice  baz fiyat (yalnızca ilanlara ve kilometreye dayanır)
     * @param exactCount     aynı marka, model ve yıldaki ilan sayısı (tüm havuzda)
     * @param extrapolated   araç, aynı model ve yıldaki ilanların kilometre aralığının dışında mı (değer daha belirsiz)
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
            long boundsMax,
            PriceMethod method,
            long baselinePrice,
            int exactCount,
            boolean extrapolated
    ) {
    }

    /** Modelin önerisi kabul edildi mi, yoksa baz fiyata mı dönüldü. */
    public record Decision(long min, long suggested, long max, boolean aiUsed) {
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

        // Aynı marka (havuz zaten tek marka), aynı model ve aynı yıl. Kilometre farkı en az olan başta.
        List<CarCandidate> exact = pool.stream()
                .filter(c -> isExactMatch(target, c))
                .sorted(Comparator.comparingInt((CarCandidate c) -> Math.abs(c.mileage() - target.mileage()))
                        .thenComparing(CarCandidate::price))
                .toList();

        List<CarCandidate> comparables = new ArrayList<>(exact.stream().limit(MAX_COMPARABLES).toList());
        for (CarCandidate c : ranked) {
            if (comparables.size() >= MAX_COMPARABLES) {
                break;
            }
            if (!exact.contains(c)) {
                comparables.add(c);
            }
        }

        int sameModelCount = (int) comparables.stream().filter(c -> isSameModel(target.model(), c.model())).count();

        long[] allPrices = comparables.stream().mapToLong(PriceMarketAnalyzer::toLong).sorted().toArray();
        long boundsMin = allPrices[0];
        long boundsMax = allPrices[allPrices.length - 1];

        if (exact.size() >= 2) {
            Baseline baseline = mileageBaseline(exact, target.mileage());
            long[] prices = exact.stream().mapToLong(PriceMarketAnalyzer::toLong).sorted().toArray();
            double band = baseline.extrapolated() ? BAND_EXTRAPOLATED : BAND_INTERPOLATED;
            return new Market(
                    comparables, sameModelCount, exact.size(),
                    prices[0], Math.round(percentile(prices, 0.5)), prices[prices.length - 1],
                    Math.round(baseline.price() * (1 - band)), Math.round(baseline.price() * (1 + band)),
                    boundsMin, boundsMax,
                    PriceMethod.MILEAGE_BASED, baseline.price(), exact.size(), baseline.extrapolated());
        }

        if (exact.size() == 1) {
            long price = toLong(exact.get(0));
            return new Market(
                    comparables, sameModelCount, 1,
                    price, price, price,
                    Math.round(price * (1 - BAND_SINGLE)), Math.round(price * (1 + BAND_SINGLE)),
                    boundsMin, boundsMax,
                    PriceMethod.SINGLE_COMPARABLE, price, 1, false);
        }

        return similarListingsMarket(target, ranked, comparables, sameModelCount, boundsMin, boundsMax);
    }

    /** Aynı model ve yıldan ilan yok: aynı markanın en benzer ilanlarının fiyat dağılımı. */
    private static Market similarListingsMarket(Target target, List<CarCandidate> ranked, List<CarCandidate> comparables,
                                                int sameModelCount, long boundsMin, long boundsMax) {
        List<CarCandidate> sameModel = ranked.stream()
                .filter(c -> isSameModel(target.model(), c.model()))
                .toList();

        // Yeterli sayıda aynı model varsa istatistik sadece onlardan çıkar, yoksa tüm benzer ilanlardan.
        List<CarCandidate> baseline = sameModel.size() >= MIN_SAME_MODEL_FOR_BASELINE ? sameModel : ranked;

        long[] prices = baseline.stream().mapToLong(PriceMarketAnalyzer::toLong).sorted().toArray();

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
                comparables, sameModelCount, baseline.size(),
                prices[0], median, prices[prices.length - 1],
                low, high,
                boundsMin, boundsMax,
                PriceMethod.SIMILAR_LISTINGS, median, 0, false);
    }

    /**
     * Modelin önerisini denetler. Makulse onu, değilse baz fiyatı ve istatistiksel aralığı döner.
     * Gemini'nin durum analizi (impact) kabul edilen fiyatın yönünü belirler (MILEAGE_BASED yönteminde).
     */
    public static Decision decide(Market market, Long min, Long suggested, Long max, ConditionImpact impact) {
        if (isPlausible(min, suggested, max, market, impact)) {
            return new Decision(min, suggested, max, true);
        }
        return new Decision(market.baselineLow(), market.baselinePrice(), market.baselineHigh(), false);
    }

    /** Modelin döndürdüğü fiyat aralığı makul ve aracın durumuyla tutarlı mı? */
    public static boolean isPlausible(Long min, Long suggested, Long max, Market market, ConditionImpact impact) {
        if (min == null || suggested == null || max == null) {
            return false;
        }
        if (min <= 0) {
            return false;
        }
        if (min > suggested || suggested > max) {
            return false;
        }

        if (market.method() != PriceMethod.MILEAGE_BASED) {
            // Baz fiyat kilometre ve yıl farkını içermiyor, model onları yorumluyor: ilan fiyat sınırlarına bakılır.
            if (min < market.boundsMin() * LOWER_BOUND_FACTOR) {
                return false;
            }
            if (max > market.boundsMax() * UPPER_BOUND_FACTOR) {
                return false;
            }
            return max <= min * MAX_RANGE_RATIO;
        }

        // MILEAGE_BASED: kilometre ve yıl baz fiyatta var, model sadece aracın durumuna göre ayarlayabilir.
        double base = market.baselinePrice();
        if (suggested < base * ANCHORED_SUGGESTED_MIN || suggested > base * ANCHORED_SUGGESTED_MAX) {
            return false;
        }
        if (min < base * ANCHORED_RANGE_MIN || max > base * ANCHORED_RANGE_MAX) {
            return false;
        }
        if (max > min * ANCHORED_MAX_RANGE_RATIO) {
            return false;
        }

        ConditionImpact direction = impact == null ? ConditionImpact.UNKNOWN : impact;
        if (direction == ConditionImpact.NEGATIVE) {
            // Daha kötü durumdaki araç, baz fiyattan pahalı önerilemez.
            return suggested <= base * (1 + DIRECTION_TOLERANCE);
        }
        if (direction == ConditionImpact.POSITIVE) {
            // Daha iyi durumdaki araç, baz fiyattan ucuz önerilemez.
            return suggested >= base * (1 - DIRECTION_TOLERANCE);
        }
        // NEUTRAL veya UNKNOWN: durum fiyatı değiştirmemeli.
        return suggested >= base * (1 - NEUTRAL_TOLERANCE) && suggested <= base * (1 + NEUTRAL_TOLERANCE);
    }

    /** Güven düzeyi: yönteme, ilan sayısına ve kilometre aralığına bağlı. Modele sorulmaz. */
    public static Level confidence(Market market) {
        Level level;
        if (market.method() == PriceMethod.MILEAGE_BASED) {
            level = market.exactCount() >= 4 ? Level.HIGH : Level.MEDIUM;
        } else if (market.method() == PriceMethod.SINGLE_COMPARABLE) {
            level = Level.LOW;
        } else {
            level = market.sameModelCount() >= 3 ? Level.MEDIUM : Level.LOW;
        }
        if (market.extrapolated()) {
            level = level == Level.HIGH ? Level.MEDIUM : Level.LOW;
        }
        return level;
    }

    // ---------- Kilometreye göre baz fiyat ----------

    record Baseline(long price, boolean extrapolated) {
    }

    /**
     * Aynı model ve yıldaki ilanların (en az 2) kilometre-fiyat ilişkisinden hedef kilometre için fiyat bulur.
     * - Hedef iki ilanın kilometresi arasındaysa: kilometre bakımından en yakın iki komşu arasında doğrusal ara değer.
     * - Hedef bir ilanla aynı kilometredeyse: o ilanın fiyatı.
     * - Hedef tüm ilanların dışındaysa: uçtaki iki ilanın eğimiyle tahmin edilir, uçtaki fiyatın %15 üstü / %15 altıyla sınırlanır.
     */
    static Baseline mileageBaseline(List<CarCandidate> exact, int targetMileage) {
        List<CarCandidate> byKm = exact.stream()
                .sorted(Comparator.comparingInt(CarCandidate::mileage).thenComparing(CarCandidate::price))
                .toList();
        int n = byKm.size();

        // upper: kilometresi hedeften büyük olan ilk ilanın sırası
        int upper = 0;
        while (upper < n && byKm.get(upper).mileage() <= targetMileage) {
            upper++;
        }

        if (upper == 0) {
            // Araç, tüm ilanlardan daha az kilometreli: ilanların en düşük kilometreli ucundan çıkış
            CarCandidate first = byKm.get(0);
            CarCandidate second = byKm.get(1);
            double base = toLong(first);
            double estimate = base + slope(first, second) * (targetMileage - first.mileage());
            return new Baseline(Math.round(clamp(estimate, base, base * EXTRAPOLATE_MAX_GAIN)), true);
        }
        if (upper == n) {
            // Araç, tüm ilanlardan daha çok kilometreli
            CarCandidate last = byKm.get(n - 1);
            CarCandidate previous = byKm.get(n - 2);
            double base = toLong(last);
            double estimate = base + slope(previous, last) * (targetMileage - last.mileage());
            return new Baseline(Math.round(clamp(estimate, base * EXTRAPOLATE_MAX_LOSS, base)), true);
        }

        CarCandidate below = byKm.get(upper - 1);
        CarCandidate above = byKm.get(upper);
        if (below.mileage() == targetMileage) {
            return new Baseline(toLong(below), false);
        }
        double ratio = (targetMileage - below.mileage()) / (double) (above.mileage() - below.mileage());
        double estimate = toLong(below) + (toLong(above) - toLong(below)) * ratio;
        return new Baseline(Math.round(estimate), false);
    }

    /** Kilometre başına fiyat değişimi (genelde negatif). Kilometreler aynıysa 0. low.mileage <= high.mileage olmalı. */
    private static double slope(CarCandidate low, CarCandidate high) {
        int deltaKm = high.mileage() - low.mileage();
        if (deltaKm == 0) {
            return 0;
        }
        return (toLong(high) - toLong(low)) / (double) deltaKm;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ---------- Benzerlik ----------

    /** Aynı model (yazım farkları hariç birebir) ve aynı model yılı. "M5" ile "M5 CS" aynı sayılmaz. */
    public static boolean isExactMatch(Target t, CarCandidate c) {
        String x = normalize(t.model());
        String y = normalize(c.model());
        return !x.isEmpty() && x.equals(y) && t.year() == c.year();
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

    /** Benzerlik sıralaması için esnek model eşleşmesi (biri diğerini içeriyorsa aynı sayılır). */
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
