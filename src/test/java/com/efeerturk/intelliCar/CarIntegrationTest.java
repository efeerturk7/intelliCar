package com.efeerturk.intelliCar;

import com.efeerturk.intelliCar.enums.CarStatus;
import com.efeerturk.intelliCar.enums.UserRole;
import com.efeerturk.intelliCar.model.Car;
import com.efeerturk.intelliCar.repository.CarRepository;
import com.efeerturk.intelliCar.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CarIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CarRepository carRepository;

    @Test
    void createCar_asLoggedInUser_returnsActiveListing() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);

        mockMvc.perform(post(API + "/car")
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson("Toyota", "Corolla", 2018, 90_000, 950_000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.viewCount").value(0));
    }

    @Test
    void createCar_withoutToken_isRejected() throws Exception {
        mockMvc.perform(post(API + "/car")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson("Toyota", "Corolla", 2018, 90_000, 950_000)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCar_withInvalidBody_isRejected() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);

        // Boş marka ve negatif fiyat
        mockMvc.perform(post(API + "/car")
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson("", "Corolla", 2018, 90_000, -5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listCars_isPublicAndFiltersByBrand() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        String brand = "Brand" + UUID.randomUUID().toString().substring(0, 8);
        createCar(seller, brand, "A", 500_000);
        createCar(seller, brand, "B", 600_000);
        createCar(seller, "Other" + UUID.randomUUID().toString().substring(0, 8), "C", 700_000);

        // Token yok: ilan listesi herkese açık
        mockMvc.perform(get(API + "/car").param("brand", brand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].brand").value(brand));
    }

    @Test
    void getCarDetail_isPublic_andStillWorksWhenServedFromRedisCache() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Honda", "Civic", 800_000);

        // 1. istek veritabanından gelir ve Redis'e yazılır, 2. istek Redis'ten okunur.
        // Not: Redis'ten okuma, RedisConfig'deki serializer'a bağlıdır. Burada 500 alınırsa sorun cache (de)serialization'dır.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get(API + "/car/" + carId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(carId.toString()))
                    .andExpect(jsonPath("$.brand").value("Honda"))
                    .andExpect(jsonPath("$.model").value("Civic"));
        }
    }

    @Test
    void getCarDetail_ofUnknownCar_isRejected() throws Exception {
        mockMvc.perform(get(API + "/car/" + UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewingCar_incrementsViewCountThroughKafka() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Renault", "Clio", 400_000);

        mockMvc.perform(get(API + "/car/" + carId)).andExpect(status().isOk());
        mockMvc.perform(get(API + "/car/" + carId)).andExpect(status().isOk());

        // Event Kafka'dan geçip CarViewConsumer tarafından işlenir, bu yüzden asenkron beklenir.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() -> {
            Car car = carRepository.findById(carId).orElseThrow();
            assertThat(car.getViewCount()).isEqualTo(2);
        });
    }

    @Test
    void updateCar_byOwner_changesPrice() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Fiat", "Egea", 450_000);

        mockMvc.perform(put(API + "/car/" + carId)
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":430000,\"mileage\":91000,\"description\":\"Fiyat düştü\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Fiyat düştü"));

        assertThat(carRepository.findById(carId).orElseThrow().getPrice()).isEqualByComparingTo("430000");
    }

    @Test
    void updateCar_byAnotherUser_isRejected() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser stranger = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Fiat", "Linea", 300_000);

        mockMvc.perform(put(API + "/car/" + carId)
                        .header("Authorization", bearer(stranger.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":1,\"mileage\":1,\"description\":\"hack\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());

        assertThat(carRepository.findById(carId).orElseThrow().getPrice()).isEqualByComparingTo("300000");
    }

    @Test
    void deleteCar_isSoftDelete() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Opel", "Astra", 350_000);

        mockMvc.perform(delete(API + "/car/" + carId).header("Authorization", bearer(seller.token())))
                .andExpect(status().isNoContent());

        // Satır silinmez, sadece INACTIVE olur
        assertThat(carRepository.findById(carId).orElseThrow().getStatus()).isEqualTo(CarStatus.INACTIVE);
    }
}
