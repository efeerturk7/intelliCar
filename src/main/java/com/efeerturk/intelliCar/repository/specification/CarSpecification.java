package com.efeerturk.intelliCar.repository.specification;

import com.efeerturk.intelliCar.dto.request.CarFilterRequest;
import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.model.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class CarSpecification {

    private CarSpecification() {
        // Yardımcı (utility) sınıf olduğu için new ile nesnesi üretilmesin
    }

    public static Specification<Car> filterByCriteria(CarFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            // SQL'deki WHERE koşullarını toplayacağımız liste
            List<Predicate> predicates = new ArrayList<>();

            // 1. ZORUNLU KURAL: Listelemede sadece aktif ilanlar gösterilsin
            predicates.add(criteriaBuilder.equal(root.get("status"), CarStatus.ACTIVE));

            // Filtre nesnesi null gelirse sadece aktif araçları dön
            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            // 2. MARKA FİLTRESİ (Büyük/küçük harf duyarsız arama)
            if (filter.brand() != null && !filter.brand().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("brand")),
                        filter.brand().toLowerCase().trim()
                ));
            }

            // 3. MODEL FİLTRESİ (İçinde geçen kelimeye göre arama - LIKE %model%)
            if (filter.model() != null && !filter.model().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("model")),
                        "%" + filter.model().toLowerCase().trim() + "%"
                ));
            }

            // 4. ŞEHİR FİLTRESİ
            if (filter.city() != null && !filter.city().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("city")),
                        filter.city().toLowerCase().trim()
                ));
            }

            // 5. MİNİMUM FİYAT (price >= minPrice)
            if (filter.minPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        filter.minPrice()
                ));
            }

            // 6. MAKSİMUM FİYAT (price <= maxPrice)
            if (filter.maxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        filter.maxPrice()
                ));
            }

            // 7. MİNİMUM YIL (year >= minYear)
            if (filter.minYear() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("year"),
                        filter.minYear()
                ));
            }

            // 8. MAKSİMUM YIL (year <= maxYear)
            if (filter.maxYear() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("year"),
                        filter.maxYear()
                ));
            }

            // 9. YAKIT TİPİ (Enum eşitliği)
            if (filter.fuelType() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("fuelType"),
                        filter.fuelType()
                ));
            }

            // 10. VİTES TİPİ (Enum eşitliği)
            if (filter.transmission() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("transmission"),
                        filter.transmission()
                ));
            }

            // Listedeki tüm şartları SQL'deki "AND" ile birbirine bağla
            // Örnek SQL: WHERE status = 'ACTIVE' AND brand = 'bmw' AND price <= 800000
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
