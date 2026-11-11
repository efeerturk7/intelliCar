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
 * Temel kural (yönlü fiyat mantığı): bir araç, başka bir ilandan hem daha yeni (veya aynı yıl) hem de daha az kilometreliyse
 * o ilandan PAHALI olmalı; hem daha eski hem daha çok kilometreliyse UCUZ olmalı. Karışık durumlarda (daha yeni ama daha çok
 * kilometreli gibi) fiyat ikisinin arasında kalır.
 *
 * Baz fiyat üç yoldan biriyle hesaplanır (PriceMethod):
 *  1) MILEAGE_BASED: aynı marka, model VE yıldan en az 2 ilan varsa, fiyat bu ilanların kilometreye göre
 *     karşılaştırılmasıyla bulunur. Örnek: 100.000 km'lik ilan 3.000.000 TL, 150.000 km'lik ilan 2.700.000 TL ise
 *     134.000 km'lik araç için fiyat bu ikisinin arasında, kilometreyle orantılı olarak hesaplanır.
 *     Yalnızca kilometre bakımından en yakın iki ilan kullanılır, uçtaki (örn. 300.000 km) ilanlar sonucu bozmaz.
 *  2) SINGLE_COMPARABLE: aynı model ve yıldan tek ilan varsa onun fiyatı, kilometre farkına göre düzeltilerek kullanılır.
 *  3) SIMILAR_LISTINGS: aynı model ve yıldan ilan yoksa, aynı modelin (yoksa aynı markanın) en benzer ilanlarının fiyatları
 *     yıl ve kilometre farkına göre düzeltilir ve medyanı alınır.
 *
 * Üç yolda da sonuç, yönlü kuralla sınırlanır (floor / ceiling): araç bir ilandan açıkça daha iyiyse onun fiyatının altına,
 * açıkça daha kötüyse üstüne çıkamaz.
 *
 * Düzeltme oranları (YEAR_RATE, KM_RATE_PER_10K) piyasa verisinden öğrenilmiş değil, varsayımdır. Yalnızca yönü ve kabaca
 * büyüklüğü verir; ilan sayısı arttıkça yerini veriden öğrenilen oranlara bırakabilir.
 *
 * Gemini bu baz fiyatı yalnızca aracın durumuna (hasar kaydı, değişen, boya) göre ayarlar. Önerisi decide() ile denetlenir;
 * tutarsızsa baz fiyat kullanılır.
 */
public final class PriceMarketAnalyzer {

    public static final int MAX_COMPARABLES = 15;

    // Düzeltme varsayımları: model yılı başına ve 10.000 km başına fiyat değişimi, her biri en fazla +-%30 etki eder.
    static final double YEAR_RATE = 0.05;
    static final double KM_RATE_PER_10K = 0.01;
    static final double ADJUSTMENT_CAP = 0.30;

    // Kilometre aralığının dışındaki araçlar için: baz fiyat, uçtaki ilanın fiyatından en fazla bu kadar sapabilir.
    private static final double EXTRAPOLATE_MAX_GAIN = 1.15;
    private static final double EXTRAPOLATE_MAX_LOSS = 0.85;

    // Baz fiyatın etrafındaki istatistiksel aralık genişliği (yöntemin güvenilirliğine göre)
    private static final double BAND_INTERPOLATED = 0.05;
    private static final double BAND_EXTRAPOLATED = 0.08;
    private static final double BAND_SINGLE = 0.10;
    private static final double BAND_SIMILAR = 0.12;
    private static final double BAND_CROSS_MODEL = 0.20;

    // Gemini'nin baz fiyattan ne kadar uzaklaşabileceği (kilometre ve yıl zaten baz fiyatta var)
    private static final double SUGGESTED_MIN = 0.65;
    private static final double SUGGESTED_MAX = 1.10;
    private static final double RANGE_MIN = 0.55;
    private static final double RANGE_MAX = 1.20;
    private static final double MAX_RANGE_RATIO = 1.4;
    // Durum bilgisi yoksa veya benzerleriyle aynıysa fiyat baz fiyata yakın kalmalı
    private static final double NEUTRAL_TOLERANCE = 0.05;
    // Yön kontrolünde küçük bir pay (yuvarlama) tanınır
    private static final double DIRECTION_TOLERANCE = 0.02;
    private static final double DOMINANCE_TOLERANCE = 0.01;

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
     * @param baselineCount  baz fiyatın dayandığı ilan sayısı
     * @param marketMin      baz fiyatın dayandığı ilanların en düşük fiyatı
     * @param marketMedian   baz fiyatın dayandığı ilanların medyan fiyatı
     * @param marketMax      baz fiyatın dayandığı ilanların en yüksek fiyatı
     * @param baselineLow    baz fiyatın etrafındaki istatistiksel aralığın alt sınırı
     * @param baselineHigh   baz fiyatın etrafındaki istatistiksel aralığın üst sınırı
     * @param method         baz fiyatın hesaplanma yöntemi
     * @param baselinePrice  baz fiyat (yalnızca ilanlara, yıla ve kilometreye dayanır; araç durumu hariç)
     * @param exactCount     aynı marka, model ve yıldaki ilan sayısı (tüm havuzda)
     * @param extrapolated   araç, aynı model ve yıldaki ilanların kilometre aralığının dışında mı (değer daha belirsiz)
     * @param floorPrice     araç, bu fiyattaki ilandan daha yeni ve daha az kilometreli: fiyat bunun altına inmemeli (yoksa null)
     * @param ceilingPrice   bu fiyattaki ilan araçtan daha yeni ve daha az kilometreli: fiyat bunun üstüne çıkmamalı (yoksa null)
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
            PriceMethod method,
            long baselinePrice,
            int exactCount,
            boolean extrapolated,
            Long floorPrice,
            Long ceilingPrice
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

        // Aynı model, herhangi bir yıl: yönlü kural (floor / ceiling) bu ilanlara bakar.
        List<CarCandidate> identicalModel = pool.stream()
                .filter(c -> isIdenticalModel(target.model(), c.model()))
                .toList();
        Bounds bounds = dominanceBounds(target, identicalModel);

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

        if (exact.size() >= 2) {
            Baseline baseline = mileageBaseline(exact, target.mileage());
            long price = applyBounds(baseline.price(), bounds);
            long[] prices = exact.stream().mapToLong(PriceMarketAnalyzer::toLong).sorted().toArray();
            double band = baseline.extrapolated() ? BAND_EXTRAPOLATED : BAND_INTERPOLATED;
            return market(comparables, sameModelCount, exact.size(), prices, price, band,
                    PriceMethod.MILEAGE_BASED, exact.size(), baseline.extrapolated(), bounds);
        }

        if (exact.size() == 1) {
            // Aynı model ve yıl: sadece kilometre farkı fiyata yansıtılır
            CarCandidate only = exact.get(0);
            long price = applyBounds(Math.round(toLong(only) * adjustmentFactor(target, only)), bounds);
            long[] prices = {toLong(only)};
            return market(comparables, sameModelCount, 1, prices, price, BAND_SINGLE,
                    PriceMethod.SINGLE_COMPARABLE, 1, false, bounds);
        }

        // Aynı model ve yıl yok: aynı modelin (yoksa markanın) en benzer ilanları, yıl ve kilometre farkına göre düzeltilir.
        List<CarCandidate> basis = identicalModel.isEmpty()
                ? ranked
                : identicalModel.stream()
                        .sorted(Comparator.comparingDouble((CarCandidate c) -> distance(target, c)))
                        .limit(MAX_COMPARABLES)
                        .toList();
        long[] rawPrices = basis.stream().mapToLong(PriceMarketAnalyzer::toLong).sorted().toArray();
        long[] adjusted = basis.stream()
                .mapToLong(c -> Math.round(toLong(c) * adjustmentFactor(target, c)))
                .sorted()
                .toArray();
        long price = applyBounds(Math.round(percentile(adjusted, 0.5)), bounds);
        double band = identicalModel.isEmpty() ? BAND_CROSS_MODEL : BAND_SIMILAR;
        return market(comparables, sameModelCount, basis.size(), rawPrices, price, band,
                PriceMethod.SIMILAR_LISTINGS, 0, false, bounds);
    }

    private static Market market(List<CarCandidate> comparables, int sameModelCount, int baselineCount, long[] sortedPrices,
                                 long price, double band, PriceMethod method, int exactCount, boolean extrapolated,
                                 Bounds bounds) {
        return new Market(
                comparables, sameModelCount, baselineCount,
                sortedPrices[0], Math.round(percentile(sortedPrices, 0.5)), sortedPrices[sortedPrices.length - 1],
                Math.round(price * (1 - band)), Math.round(price * (1 + band)),
                method, price, exactCount, extrapolated,
                bounds.floor(), bounds.ceiling());
    }

    // ---------- Yönlü kural: yıl ve kilometreye göre fiyat sınırları ----------

    record Bounds(Long floor, Long ceiling) {
    }

    /**
     * Aynı modeldeki ilanlara göre fiyatın alt ve üst sınırı.
     * - Araç bir ilandan daha yeni (veya aynı yıl) VE daha az (veya eşit) kilometreliyse o ilandan pahalı olmalı: floor.
     * - Bir ilan araçtan daha yeni (veya aynı yıl) VE daha az kilometreliyse araç ondan ucuz olmalı: ceiling.
     * Karışık durumlarda (daha yeni ama daha çok kilometreli gibi) ilan sınır koymaz.
     */
    static Bounds dominanceBounds(Target target, List<CarCandidate> sameModel) {
        Long floor = null;
        Long ceiling = null;
        for (CarCandidate c : sameModel) {
            long price = toLong(c);
            boolean targetBetter = target.year() >= c.year() && target.mileage() <= c.mileage();
            boolean listingBetter = c.year() >= target.year() && c.mileage() <= target.mileage();
            if (targetBetter) {
                floor = floor == null ? price : Math.max(floor, price);
            }
            if (listingBetter) {
                ceiling = ceiling == null ? price : Math.min(ceiling, price);
            }
        }
        return new Bounds(floor, ceiling);
    }

    /** Fiyatı sınırların içine alır. Sınırlar birbiriyle çelişiyorsa (tutarsız ilan verisi) fiyata dokunmaz. */
    static long applyBounds(long price, Bounds bounds) {
        Long floor = bounds.floor();
        Long ceiling = bounds.ceiling();
        if (floor != null && ceiling != null && floor > ceiling) {
            return price;
        }
        long result = price;
        if (floor != null && result < floor) {
            result = floor;
        }
        if (ceiling != null && result > ceiling) {
            result = ceiling;
        }
        return result;
    }

    /**
     * Bir ilanın fiyatını, satılacak aracın yılına ve kilometresine göre düzeltme çarpanı.
     * Araç daha yeniyse ve daha az kilometreliyse çarpan 1'den büyük, tersiyse küçüktür.
     */
    static double adjustmentFactor(Target target, CarCandidate c) {
        double yearEffect = clamp(YEAR_RATE * (target.year() - c.year()), -ADJUSTMENT_CAP, ADJUSTMENT_CAP);
        double kmEffect = clamp(KM_RATE_PER_10K * (c.mileage() - target.mileage()) / 10_000.0,
                -ADJUSTMENT_CAP, ADJUSTMENT_CAP);
        return (1 + yearEffect) * (1 + kmEffect);
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

    /** Modelin döndürdüğü fiyat aralığı makul, yönlü kuralla ve aracın durumuyla tutarlı mı? */
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

        // Kilometre ve yıl farkı baz fiyatta zaten var: model sadece aracın durumuna göre ayarlayabilir.
        double base = market.baselinePrice();
        if (suggested < base * SUGGESTED_MIN || suggested > base * SUGGESTED_MAX) {
            return false;
        }
        if (min < base * RANGE_MIN || max > base * RANGE_MAX) {
            return false;
        }
        if (max > min * MAX_RANGE_RATIO) {
            return false;
        }

        ConditionImpact direction = impact == null ? ConditionImpact.UNKNOWN : impact;
        Long floor = market.floorPrice();
        Long ceiling = market.ceilingPrice();

        if (direction == ConditionImpact.NEGATIVE) {
            // Daha kötü durumdaki araç, baz fiyattan pahalı önerilemez. Alt sınır (floor) uygulanmaz: hasar fiyatı düşürebilir.
            return suggested <= base * (1 + DIRECTION_TOLERANCE);
        }
        if (direction == ConditionImpact.POSITIVE) {
            // Daha iyi durumdaki araç, baz fiyattan ucuz önerilemez ve daha eski/çok kilometreli ilandan ucuz olamaz.
            if (suggested < base * (1 - DIRECTION_TOLERANCE)) {
                return false;
            }
            return floor == null || suggested >= floor * (1 - DOMINANCE_TOLERANCE);
        }

        // NEUTRAL veya UNKNOWN: durum fiyatı değiştirmemeli, yönlü kural aynen geçerli.
        if (suggested < base * (1 - NEUTRAL_TOLERANCE) || suggested > base * (1 + NEUTRAL_TOLERANCE)) {
            return false;
        }
        if (floor != null && suggested < floor * (1 - DOMINANCE_TOLERANCE)) {
            return false;
        }
        return ceiling == null || suggested <= ceiling * (1 + DOMINANCE_TOLERANCE);
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
        return isIdenticalModel(t.model(), c.model()) && t.year() == c.year();
    }

    /** Model adı büyük/küçük harf ve fazla boşluk dışında birebir aynı mı? */
    static boolean isIdenticalModel(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        return !x.isEmpty() && x.equals(y);
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
