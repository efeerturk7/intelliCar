package com.efeerturk.intelliCar;

import com.efeerturk.intelliCar.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Uygulama gerçek PostgreSQL, Redis ve Kafka ile ayağa kalkabiliyor mu?
 * (Varsayılan contextLoads testi altyapı olmadan çalışmazdı, bu yüzden ortak tabanı kullanıyor.)
 */
class IntelliCarApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
