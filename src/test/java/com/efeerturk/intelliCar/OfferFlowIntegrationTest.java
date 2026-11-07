package com.efeerturk.intelliCar;

import com.efeerturk.intelliCar.enums.OfferStatus;
import com.efeerturk.intelliCar.enums.UserRole;
import com.efeerturk.intelliCar.repository.OfferRepository;
import com.efeerturk.intelliCar.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teklif akışı: veritabanı + Kafka + e-posta bildirimi birlikte test edilir.
 * HTTP isteği -> DB -> Kafka -> consumer -> JavaMailSender (mock) zincirinin tamamı çalışır.
 */
class OfferFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OfferRepository offerRepository;

    @Test
    void newOffer_isCreatedAsPending_andSellerIsEmailed() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Toyota", "Corolla", 1_100_000);

        mockMvc.perform(post(API + "/offer")
                        .header("Authorization", bearer(buyer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":\"%s\",\"offeredPrice\":1050000,\"note\":\"Peşin öderim\"}".formatted(carId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.carId").value(carId.toString()));

        SimpleMailMessage mail = awaitMailTo(seller.email());
        assertThat(mail.getSubject()).isEqualTo("Yeni teklif: Toyota Corolla");
        assertThat(mail.getText()).contains("1.050.000");
    }

    @Test
    void acceptingOffer_emailsBuyer() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Honda", "Civic", 900_000);
        UUID offerId = createOffer(buyer, carId, 850_000);

        mockMvc.perform(patch(API + "/offer/" + offerId + "/respond")
                        .param("newStatus", "ACCEPTED")
                        .header("Authorization", bearer(seller.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        SimpleMailMessage mail = awaitMailTo(buyer.email());
        assertThat(mail.getSubject()).isEqualTo("Teklifiniz kabul edildi: Honda Civic");
        assertThat(mail.getText()).contains("850.000");
    }

    @Test
    void rejectingOffer_emailsBuyer() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Ford", "Focus", 700_000);
        UUID offerId = createOffer(buyer, carId, 500_000);

        mockMvc.perform(patch(API + "/offer/" + offerId + "/respond")
                        .param("newStatus", "REJECTED")
                        .header("Authorization", bearer(seller.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        SimpleMailMessage mail = awaitMailTo(buyer.email());
        assertThat(mail.getSubject()).isEqualTo("Teklifiniz reddedildi: Ford Focus");
    }

    @Test
    void offerOnOwnCar_isRejected() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        UUID carId = createCar(seller, "Kia", "Ceed", 600_000);

        mockMvc.perform(post(API + "/offer")
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":\"%s\",\"offeredPrice\":500000}".formatted(carId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void secondPendingOfferFromSameBuyer_isRejected() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Hyundai", "i20", 500_000);
        createOffer(buyer, carId, 450_000);

        mockMvc.perform(post(API + "/offer")
                        .header("Authorization", bearer(buyer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":\"%s\",\"offeredPrice\":460000}".formatted(carId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheSeller_canRespondToOffer() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Skoda", "Octavia", 800_000);
        UUID offerId = createOffer(buyer, carId, 750_000);

        mockMvc.perform(patch(API + "/offer/" + offerId + "/respond")
                        .param("newStatus", "ACCEPTED")
                        .header("Authorization", bearer(buyer.token())))
                .andExpect(status().isBadRequest());

        assertThat(offerRepository.findById(offerId).orElseThrow().getStatus()).isEqualTo(OfferStatus.PENDING);
    }

    @Test
    void offersOfACar_areVisibleOnlyToItsOwner() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Seat", "Leon", 650_000);
        createOffer(buyer, carId, 600_000);

        mockMvc.perform(get(API + "/offer/cars/" + carId).header("Authorization", bearer(seller.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get(API + "/offer/cars/" + carId).header("Authorization", bearer(buyer.token())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myOffers_listsOffersOfTheBuyer() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Peugeot", "208", 550_000);
        UUID offerId = createOffer(buyer, carId, 500_000);

        mockMvc.perform(get(API + "/offer/my-offers").header("Authorization", bearer(buyer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(offerId.toString()));
    }

    @Test
    void whenCarIsSold_pendingOffersExpireThroughKafka() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Dacia", "Duster", 750_000);
        UUID offerId = createOffer(buyer, carId, 700_000);

        updateCar(seller, carId, 750_000, "SOLD");

        // car-status-events -> CarStatusConsumer -> bekleyen teklifler EXPIRED
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).untilAsserted(() ->
                assertThat(offerRepository.findById(offerId).orElseThrow().getStatus()).isEqualTo(OfferStatus.EXPIRED));
    }

    @Test
    void offerOnSoldCar_isRejected() throws Exception {
        TestUser seller = registerUser(UserRole.SELLER);
        TestUser buyer = registerUser(UserRole.BUYER);
        UUID carId = createCar(seller, "Nissan", "Qashqai", 1_000_000);
        updateCar(seller, carId, 1_000_000, "SOLD");

        mockMvc.perform(post(API + "/offer")
                        .header("Authorization", bearer(buyer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\":\"%s\",\"offeredPrice\":900000}".formatted(carId)))
                .andExpect(status().isBadRequest());
    }
}
