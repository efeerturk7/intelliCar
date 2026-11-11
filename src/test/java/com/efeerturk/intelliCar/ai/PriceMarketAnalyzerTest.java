package com.efeerturk.intelliCar.ai;

import com.efeerturk.intelliCar.enums.FuelType;
import com.efeerturk.intelliCar.enums.Transmission;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fiyat tahmininin sayısal kısmının testleri. Docker, Spring veya yapay zeka gerekmez.
 * Ana senaryo: kullanıcı "BMW M5 CS, 2020, 134.000 km" girer; veritabanında aynı model ve yıldan
 * 100.000 km, 150.000 km ve 300.000 km'lik üç ilan vardır.
 */
class PriceMarketAnalyzerTest {

    private static final PriceMarketAnalyzer.Target M5_CS_2020_134K =
            new PriceMarketAnalyzer.Target("M5 CS", 2020, 134_000, FuelType.GASOLINE, Transmission.AUTOMATIC, "Istanbul");

    private static CarCandidate car(String model, int year, int mileage, long price) {
        return new CarCandidate(UUID.randomUUID(), "BMW", model, year, mileage,
                BigDecimal.valueOf(price), FuelType.GASOLINE, Transmission.AUTOMATIC, "Istanbul", "açıklama", null);
    }

    private static PriceMarketAnalyzer.Target target(int mileage) {
        return new PriceMarketAnalyzer.Target("M5 CS", 2020, mileage, FuelType.GASOLINE, Transmission.AUTOMATIC, "Istanbul");
    }

    /** İstenen ana senaryo: 100.000 km = 3.000.000 TL, 150.000 km = 2.700.000 TL, 300.000 km = 2.200.000 TL */
    private static List<CarCandidate> threeM5Listings() {
        return List.of(
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5 CS", 2020, 300_000, 2_200_000),
                car("M5 CS", 2020, 150_000, 2_700_000));
    }

    // ---------- Kilometreye göre baz fiyat ----------

    @Test
    void priceIsBetweenTheTwoNearestListingsByMileage() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, threeM5Listings());

        assertThat(market.method()).isEqualTo(PriceMethod.MILEAGE_BASED);
        assertThat(market.exactCount()).isEqualTo(3);
        assertThat(market.extrapolated()).isFalse();
        // 134.000 km: 100.000 km'den daha ucuz, 150.000 km'den daha pahalı
        assertThat(market.baselinePrice()).isLessThan(3_000_000L);
        assertThat(market.baselinePrice()).isGreaterThan(2_700_000L);
        // 3.000.000 - 300.000 * (34.000 / 50.000) = 2.796.000
        assertThat(market.baselinePrice()).isEqualTo(2_796_000L);
    }

    @Test
    void listingFarAwayInMileageDoesNotAffectThePrice() {
        List<CarCandidate> withCheap300k = List.of(
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5 CS", 2020, 150_000, 2_700_000),
                car("M5 CS", 2020, 300_000, 2_200_000));
        List<CarCandidate> with300kAlmostFree = List.of(
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5 CS", 2020, 150_000, 2_700_000),
                car("M5 CS", 2020, 300_000, 1_000_000));

        long first = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, withCheap300k).baselinePrice();
        long second = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, with300kAlmostFree).baselinePrice();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void statisticalRangeIsAroundTheBasePrice() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, threeM5Listings());

        // Kilometre aralığı içinde: baz fiyatın yüzde 5 altı ve üstü
        assertThat(market.baselineLow()).isEqualTo(2_656_200L);
        assertThat(market.baselineHigh()).isEqualTo(2_935_800L);
        assertThat(market.marketMin()).isEqualTo(2_200_000L);
        assertThat(market.marketMax()).isEqualTo(3_000_000L);
    }

    @Test
    void sameMileageAsAListingGivesThatListingsPrice() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(target(100_000), threeM5Listings());

        assertThat(market.baselinePrice()).isEqualTo(3_000_000L);
        assertThat(market.extrapolated()).isFalse();
    }

    @Test
    void carWithLessMileageThanAllListingsIsPricedHigherButCapped() {
        // 50.000 km: eğim -6 TL/km, 100.000 km'deki fiyat 3.000.000 => 3.300.000
        PriceMarketAnalyzer.Market near = PriceMarketAnalyzer.analyze(target(50_000), threeM5Listings());
        assertThat(near.extrapolated()).isTrue();
        assertThat(near.baselinePrice()).isEqualTo(3_300_000L);

        // 0 km: hesap 3.600.000 çıkar ama en yakın ilanın fiyatının yüzde 15 üstüyle sınırlanır
        PriceMarketAnalyzer.Market zero = PriceMarketAnalyzer.analyze(target(0), threeM5Listings());
        assertThat(zero.baselinePrice()).isEqualTo(3_450_000L);
    }

    @Test
    void carWithMoreMileageThanAllListingsIsPricedLowerButCapped() {
        // 400.000 km: en çok kilometreli ilan 300.000 km = 2.200.000. Eğim (2.200.000 - 2.700.000) / 150.000 = -3,33 TL/km
        PriceMarketAnalyzer.Market far = PriceMarketAnalyzer.analyze(target(400_000), threeM5Listings());
        assertThat(far.extrapolated()).isTrue();
        assertThat(far.baselinePrice()).isLessThan(2_200_000L);
        assertThat(far.baselinePrice()).isGreaterThanOrEqualTo(1_870_000L); // 2.200.000 * 0,85

        // Çok uç bir kilometre: negatif fiyat çıkmaz, alt sınırda kalır
        PriceMarketAnalyzer.Market extreme = PriceMarketAnalyzer.analyze(target(5_000_000), threeM5Listings());
        assertThat(extreme.baselinePrice()).isEqualTo(1_870_000L);
    }

    @Test
    void listingsWithTheSameMileageDoNotBreakTheCalculation() {
        List<CarCandidate> pool = List.of(
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5 CS", 2020, 100_000, 2_900_000));

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(target(150_000), pool);

        assertThat(market.method()).isEqualTo(PriceMethod.MILEAGE_BASED);
        // Araç iki ilandan da daha çok kilometreli: ucuz olan ilanın fiyatını (2.900.000) aşamaz
        assertThat(market.baselinePrice()).isEqualTo(2_900_000L);
    }

    // ---------- Hangi ilanlar "aynı" sayılır ----------

    @Test
    void differentModelOrYearIsNotAnExactMatch() {
        List<CarCandidate> pool = List.of(
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5", 2020, 120_000, 2_000_000),          // farklı model (M5 CS değil)
                car("M5 CS", 2019, 130_000, 2_500_000));      // farklı yıl

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, pool);

        assertThat(market.exactCount()).isEqualTo(1);
        assertThat(market.method()).isEqualTo(PriceMethod.SINGLE_COMPARABLE);
        // 134.000 km, ilandan (100.000 km) 34.000 km fazla: fiyat kilometre farkına göre biraz düşer
        assertThat(market.baselinePrice()).isEqualTo(2_898_000L);
        // Model yine de diğer ilanları görür (karşılaştırma bağlamı için)
        assertThat(market.comparables()).hasSize(3);
    }

    @Test
    void modelNameMatchIgnoresCaseAndExtraSpaces() {
        List<CarCandidate> pool = List.of(
                car("m5  cs", 2020, 100_000, 3_000_000),
                car(" M5 CS ", 2020, 150_000, 2_700_000));

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, pool);

        assertThat(market.exactCount()).isEqualTo(2);
        assertThat(market.method()).isEqualTo(PriceMethod.MILEAGE_BASED);
    }

    @Test
    void exactListingsComeFirstInTheComparables() {
        List<CarCandidate> pool = List.of(
                car("M5", 2020, 134_000, 2_000_000),
                car("M5 CS", 2020, 300_000, 2_200_000),
                car("M5 CS", 2020, 100_000, 3_000_000));

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, pool);

        assertThat(PriceMarketAnalyzer.isExactMatch(M5_CS_2020_134K, market.comparables().get(0))).isTrue();
        assertThat(PriceMarketAnalyzer.isExactMatch(M5_CS_2020_134K, market.comparables().get(1))).isTrue();
        assertThat(PriceMarketAnalyzer.isExactMatch(M5_CS_2020_134K, market.comparables().get(2))).isFalse();
    }

    @Test
    void withoutSameModelAndYearItFallsBackToSimilarListings() {
        List<CarCandidate> pool = List.of(
                car("M5 CS", 2018, 90_000, 2_500_000),
                car("M5 CS", 2019, 110_000, 2_800_000),
                car("M5 CS", 2021, 40_000, 3_500_000));

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, pool);

        assertThat(market.method()).isEqualTo(PriceMethod.SIMILAR_LISTINGS);
        assertThat(market.exactCount()).isEqualTo(0);
        // Yıl ve kilometre farkına göre düzeltilmiş ilan fiyatlarının medyanı (2019/110.000 km ilanı ortada)
        assertThat(market.baselinePrice()).isGreaterThan(2_800_000L);
        assertThat(market.baselinePrice()).isLessThan(3_000_000L);
        // 2021 / 40.000 km ilan hem daha yeni hem daha az kilometreli: araç ondan pahalı olamaz
        assertThat(market.ceilingPrice()).isEqualTo(3_500_000L);
        assertThat(market.floorPrice() == null).isTrue();
    }

    @Test
    void emptyPoolIsRejected() {
        assertThatThrownBy(() -> PriceMarketAnalyzer.analyze(M5_CS_2020_134K, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Güven düzeyi ----------

    @Test
    void confidenceDependsOnMethodAndNumberOfListings() {
        List<CarCandidate> four = List.of(
                car("M5 CS", 2020, 80_000, 3_100_000),
                car("M5 CS", 2020, 100_000, 3_000_000),
                car("M5 CS", 2020, 150_000, 2_700_000),
                car("M5 CS", 2020, 170_000, 2_600_000));

        assertThat(PriceMarketAnalyzer.confidence(PriceMarketAnalyzer.analyze(M5_CS_2020_134K, four)))
                .isEqualTo(PriceMarketAnalyzer.Level.HIGH);
        assertThat(PriceMarketAnalyzer.confidence(PriceMarketAnalyzer.analyze(M5_CS_2020_134K, threeM5Listings())))
                .isEqualTo(PriceMarketAnalyzer.Level.MEDIUM);
        // Kilometre aralığının dışındaki araç bir kademe düşer
        assertThat(PriceMarketAnalyzer.confidence(PriceMarketAnalyzer.analyze(target(10_000), four)))
                .isEqualTo(PriceMarketAnalyzer.Level.MEDIUM);
        assertThat(PriceMarketAnalyzer.confidence(PriceMarketAnalyzer.analyze(target(10_000), threeM5Listings())))
                .isEqualTo(PriceMarketAnalyzer.Level.LOW);
    }

    @Test
    void singleListingAndSimilarListingsGiveLowerConfidence() {
        PriceMarketAnalyzer.Market single = PriceMarketAnalyzer.analyze(M5_CS_2020_134K,
                List.of(car("M5 CS", 2020, 100_000, 3_000_000)));
        assertThat(PriceMarketAnalyzer.confidence(single)).isEqualTo(PriceMarketAnalyzer.Level.LOW);

        PriceMarketAnalyzer.Market similarFew = PriceMarketAnalyzer.analyze(M5_CS_2020_134K,
                List.of(car("M3", 2018, 90_000, 2_000_000)));
        assertThat(PriceMarketAnalyzer.confidence(similarFew)).isEqualTo(PriceMarketAnalyzer.Level.LOW);

        PriceMarketAnalyzer.Market similarMany = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, List.of(
                car("M5 CS", 2018, 90_000, 2_500_000),
                car("M5 CS", 2019, 110_000, 2_800_000),
                car("M5 CS", 2021, 40_000, 3_500_000)));
        assertThat(PriceMarketAnalyzer.confidence(similarMany)).isEqualTo(PriceMarketAnalyzer.Level.MEDIUM);
    }

    // ---------- Gemini önerisinin denetimi (hasar, değişen, boya) ----------

    private static PriceMarketAnalyzer.Market anchoredMarket() {
        return PriceMarketAnalyzer.analyze(M5_CS_2020_134K, threeM5Listings()); // baz fiyat 2.796.000
    }

    @Test
    void worseConditionLowersThePriceAndIsAccepted() {
        PriceMarketAnalyzer.Decision decision = PriceMarketAnalyzer.decide(
                anchoredMarket(), 2_350_000L, 2_500_000L, 2_650_000L, ConditionImpact.NEGATIVE);

        assertThat(decision.aiUsed()).isTrue();
        assertThat(decision.suggested()).isEqualTo(2_500_000L);
    }

    @Test
    void worseConditionWithAHigherPriceIsRejected() {
        PriceMarketAnalyzer.Market market = anchoredMarket();

        PriceMarketAnalyzer.Decision decision = PriceMarketAnalyzer.decide(
                market, 2_800_000L, 2_900_000L, 3_000_000L, ConditionImpact.NEGATIVE);

        // Tutarsız: hasarlı araç baz fiyattan pahalı. Baz fiyata dönülür.
        assertThat(decision.aiUsed()).isFalse();
        assertThat(decision.suggested()).isEqualTo(2_796_000L);
        assertThat(decision.min()).isEqualTo(market.baselineLow());
        assertThat(decision.max()).isEqualTo(market.baselineHigh());
    }

    @Test
    void betterConditionRaisesThePriceAndIsAccepted() {
        PriceMarketAnalyzer.Decision decision = PriceMarketAnalyzer.decide(
                anchoredMarket(), 2_750_000L, 2_900_000L, 3_050_000L, ConditionImpact.POSITIVE);

        assertThat(decision.aiUsed()).isTrue();
    }

    @Test
    void betterConditionWithALowerPriceIsRejected() {
        PriceMarketAnalyzer.Decision decision = PriceMarketAnalyzer.decide(
                anchoredMarket(), 2_450_000L, 2_600_000L, 2_750_000L, ConditionImpact.POSITIVE);

        assertThat(decision.aiUsed()).isFalse();
    }

    @Test
    void unknownConditionMustStayCloseToTheBasePrice() {
        PriceMarketAnalyzer.Market market = anchoredMarket();

        assertThat(PriceMarketAnalyzer.decide(market, 2_600_000L, 2_750_000L, 2_900_000L, ConditionImpact.UNKNOWN).aiUsed())
                .isTrue();
        // Durum bilgisi yokken fiyatı yüzde 18 düşürmek için sebep yok
        assertThat(PriceMarketAnalyzer.decide(market, 2_100_000L, 2_300_000L, 2_450_000L, ConditionImpact.UNKNOWN).aiUsed())
                .isFalse();
        assertThat(PriceMarketAnalyzer.decide(market, 2_600_000L, 2_750_000L, 2_900_000L, ConditionImpact.NEUTRAL).aiUsed())
                .isTrue();
    }

    @Test
    void absurdOrBrokenAnswersFallBackToTheBasePrice() {
        PriceMarketAnalyzer.Market market = anchoredMarket();

        // Çok yüksek
        assertThat(PriceMarketAnalyzer.decide(market, 4_000_000L, 5_000_000L, 6_000_000L, ConditionImpact.POSITIVE).aiUsed()).isFalse();
        // Eksik değerler
        assertThat(PriceMarketAnalyzer.decide(market, null, null, null, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
        // Sırasız değerler
        assertThat(PriceMarketAnalyzer.decide(market, 2_900_000L, 2_700_000L, 2_800_000L, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
        // Sıfır veya negatif
        assertThat(PriceMarketAnalyzer.decide(market, 0L, 2_700_000L, 2_800_000L, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
        // Çok geniş aralık
        assertThat(PriceMarketAnalyzer.decide(market, 1_700_000L, 2_500_000L, 2_800_000L, ConditionImpact.NEGATIVE).aiUsed()).isFalse();
    }

    @Test
    void withoutSameModelAndYearTheModelAdjustsForYearAndMileageWithinListingBounds() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(M5_CS_2020_134K, List.of(
                car("M5 CS", 2018, 90_000, 2_500_000),
                car("M5 CS", 2019, 110_000, 2_800_000),
                car("M5 CS", 2021, 40_000, 3_500_000)));

        assertThat(PriceMarketAnalyzer.decide(market, 2_800_000L, 3_000_000L, 3_200_000L, ConditionImpact.UNKNOWN).aiUsed()).isTrue();
        // İlanların en pahalısının çok üstü
        assertThat(PriceMarketAnalyzer.decide(market, 4_000_000L, 5_000_000L, 5_500_000L, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
        // En ucuzun çok altı
        assertThat(PriceMarketAnalyzer.decide(market, 1_000_000L, 1_500_000L, 1_800_000L, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
    }

    // ---------- Yönlü kural: yıl ve kilometre (Audi A4 örneği) ----------

    private static CarCandidate audi(int year, int mileage, long price) {
        return new CarCandidate(UUID.randomUUID(), "Audi", "A4", year, mileage,
                BigDecimal.valueOf(price), FuelType.GASOLINE, Transmission.AUTOMATIC, "Konya", "açıklama", null);
    }

    private static PriceMarketAnalyzer.Target audiTarget(int year, int mileage) {
        return new PriceMarketAnalyzer.Target("A4", year, mileage, FuelType.GASOLINE, Transmission.AUTOMATIC, "Istanbul");
    }

    @Test
    void newerAndLowerMileageCarIsPricedAboveTheListing() {
        // İlan: A4 2020, 119.500 km, 1.760.000 TL. Araç: A4 2021, 0 km.
        List<CarCandidate> pool = List.of(audi(2020, 119_500, 1_760_000));

        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(audiTarget(2021, 0), pool);

        assertThat(market.method()).isEqualTo(PriceMethod.SIMILAR_LISTINGS);
        assertThat(market.baselinePrice()).isGreaterThan(1_760_000L);
        assertThat(market.floorPrice()).isEqualTo(1_760_000L);
        // Aralık makul genişlikte olmalı (eskiden 1.078.000 - 2.889.000 çıkıyordu)
        assertThat(market.baselineHigh()).isLessThan(market.baselinePrice() * 3 / 2);
        assertThat(market.baselineLow()).isGreaterThan(market.baselinePrice() / 2);
    }

    @Test
    void aSuggestionBelowTheListingIsRejectedForTheNewerCar() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(audiTarget(2021, 0),
                List.of(audi(2020, 119_500, 1_760_000)));

        // Önceki hatalı sonuç: 1.625.000 TL (aralık 1.078.000 - 2.889.000)
        assertThat(PriceMarketAnalyzer.decide(market, 1_078_000L, 1_625_000L, 2_889_000L, ConditionImpact.UNKNOWN).aiUsed()).isFalse();
        // Daha yeni ve 0 km olan araç için 1.600.000 TL de kabul edilmez
        assertThat(PriceMarketAnalyzer.decide(market, 1_500_000L, 1_600_000L, 1_700_000L, ConditionImpact.NEUTRAL).aiUsed()).isFalse();
    }

    @Test
    void fourQuadrantsFollowTheExpectedOrder() {
        // Referans ilan: A4 2020, 100.000 km, 2.000.000 TL
        List<CarCandidate> pool = List.of(audi(2020, 100_000, 2_000_000));

        long newLowKm = PriceMarketAnalyzer.analyze(audiTarget(2021, 20_000), pool).baselinePrice();
        long newHighKm = PriceMarketAnalyzer.analyze(audiTarget(2021, 180_000), pool).baselinePrice();
        long oldLowKm = PriceMarketAnalyzer.analyze(audiTarget(2019, 20_000), pool).baselinePrice();
        long oldHighKm = PriceMarketAnalyzer.analyze(audiTarget(2019, 180_000), pool).baselinePrice();

        assertThat(newLowKm).isGreaterThan(newHighKm);
        assertThat(newLowKm).isGreaterThan(oldLowKm);
        assertThat(newHighKm).isGreaterThan(oldHighKm);
        assertThat(oldLowKm).isGreaterThan(oldHighKm);
        // Hem eski hem çok kilometreli: ilandan ucuz. Hem yeni hem az kilometreli: ilandan pahalı.
        assertThat(oldHighKm).isLessThan(2_000_000L);
        assertThat(newLowKm).isGreaterThan(2_000_000L);
    }

    @Test
    void olderAndHigherMileageCarIsPricedBelowTheListing() {
        PriceMarketAnalyzer.Market market = PriceMarketAnalyzer.analyze(audiTarget(2019, 180_000),
                List.of(audi(2020, 100_000, 2_000_000)));

        assertThat(market.ceilingPrice()).isEqualTo(2_000_000L);
        assertThat(market.baselinePrice()).isLessThan(2_000_000L);
    }

    @Test
    void dominanceBoundsIgnoreListingsThatAreMixed() {
        // Daha yeni ama daha çok kilometreli ilan, ne alt ne üst sınır koyar
        PriceMarketAnalyzer.Bounds bounds = PriceMarketAnalyzer.dominanceBounds(audiTarget(2020, 50_000),
                List.of(audi(2021, 90_000, 2_400_000), audi(2019, 20_000, 1_900_000)));

        assertThat(bounds.floor() == null).isTrue();
        assertThat(bounds.ceiling() == null).isTrue();
    }

    @Test
    void applyBoundsKeepsThePriceWhenBoundsContradict() {
        assertThat(PriceMarketAnalyzer.applyBounds(2_000_000L, new PriceMarketAnalyzer.Bounds(2_500_000L, 1_800_000L)))
                .isEqualTo(2_000_000L);
        assertThat(PriceMarketAnalyzer.applyBounds(1_000_000L, new PriceMarketAnalyzer.Bounds(1_500_000L, null)))
                .isEqualTo(1_500_000L);
        assertThat(PriceMarketAnalyzer.applyBounds(3_000_000L, new PriceMarketAnalyzer.Bounds(null, 2_500_000L)))
                .isEqualTo(2_500_000L);
    }

    @Test
    void singleListingWithSameYearIsAdjustedByMileage() {
        PriceMarketAnalyzer.Market lowKm = PriceMarketAnalyzer.analyze(audiTarget(2020, 50_000),
                List.of(audi(2020, 100_000, 2_000_000)));
        PriceMarketAnalyzer.Market highKm = PriceMarketAnalyzer.analyze(audiTarget(2020, 150_000),
                List.of(audi(2020, 100_000, 2_000_000)));

        assertThat(lowKm.method()).isEqualTo(PriceMethod.SINGLE_COMPARABLE);
        assertThat(lowKm.baselinePrice()).isGreaterThan(2_000_000L);
        assertThat(highKm.baselinePrice()).isLessThan(2_000_000L);
    }

    // ---------- ConditionImpact ----------

    @Test
    void conditionImpactParsingIsForgiving() {
        assertThat(ConditionImpact.parse("negative")).isEqualTo(ConditionImpact.NEGATIVE);
        assertThat(ConditionImpact.parse(" Positive ")).isEqualTo(ConditionImpact.POSITIVE);
        assertThat(ConditionImpact.parse("NEUTRAL")).isEqualTo(ConditionImpact.NEUTRAL);
        assertThat(ConditionImpact.parse("kötü")).isEqualTo(ConditionImpact.UNKNOWN);
        assertThat(ConditionImpact.parse("")).isEqualTo(ConditionImpact.UNKNOWN);
        assertThat(ConditionImpact.parse(null)).isEqualTo(ConditionImpact.UNKNOWN);
    }
}
