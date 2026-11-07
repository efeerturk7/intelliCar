package com.efeerturk.intelliCar.support;

import com.efeerturk.intelliCar.enums.UserRole;
import com.efeerturk.intelliCar.service.AiRecommendationService;
import com.efeerturk.intelliCar.service.PriceEstimationService;
import com.jayway.jsonpath.JsonPath;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tüm entegrasyon testlerinin ortak tabanı.
 *
 * - PostgreSQL, Redis ve Kafka gerçek container olarak bir kez başlatılır ve tüm test sınıfları tarafından paylaşılır.
 *   (Container'ları JVM kapanırken Testcontainers'ın Ryuk container'ı temizler.)
 * - Tüm test sınıfları aynı Spring context'ini kullanır. Bu önemli: her context kendi Kafka consumer'larını açar,
 *   ikinci bir context aynı consumer grubuna katılıp mesajları çalardı. Bu yüzden @MockitoBean'ler sadece burada tanımlıdır;
 *   alt sınıflarda @TestPropertySource, @MockitoBean vb. EKLEME.
 * - Testler birbirinden bağımsız olsun diye her test kendi rastgele kullanıcılarını ve ilanlarını oluşturur.
 * - Gemini'ye hiç gidilmez (AI servisleri mock). E-posta gönderilmez (JavaMailSender mock), gönderilen mailler doğrulanır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.0");

    static {
        Startables.deepStart(Stream.of(POSTGRES, REDIS, KAFKA)).join();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JavaMailSender mailSender;
    @MockitoBean
    protected AiRecommendationService aiRecommendationService;
    @MockitoBean
    protected PriceEstimationService priceEstimationService;

    protected static final String API = "/intelliCar/api/v1";

    protected record TestUser(UUID id, String email, String token) {
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Rastgele e-postalı yeni bir kullanıcı kaydeder ve token'ını döner. */
    protected TestUser registerUser(UserRole role) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.local";
        MvcResult result = mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", role)))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.userId");
        String token = JsonPath.read(json, "$.accessToken");
        return new TestUser(UUID.fromString(id), email, token);
    }

    protected static String registerJson(String email, String password, UserRole role) {
        return """
                {"email":"%s","password":"%s","firstName":"Test","lastName":"User","phone":"5551234567","role":"%s"}
                """.formatted(email, password, role);
    }

    protected static String carJson(String brand, String model, int year, int mileage, long price) {
        return """
                {"brand":"%s","model":"%s","year":%d,"mileage":%d,"price":%d,"fuelType":"GASOLINE",
                 "transmission":"AUTOMATIC","city":"Istanbul","description":"Test ilanı"}
                """.formatted(brand, model, year, mileage, price);
    }

    /** Satıcı adına ilan oluşturur, ilanın id'sini döner. */
    protected UUID createCar(TestUser seller, String brand, String model, long price) throws Exception {
        MvcResult result = mockMvc.perform(post(API + "/car")
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carJson(brand, model, 2018, 90_000, price)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    /** Alıcı adına teklif verir, teklifin id'sini döner. */
    protected UUID createOffer(TestUser buyer, UUID carId, long price) throws Exception {
        MvcResult result = mockMvc.perform(post(API + "/offer")
                        .header("Authorization", bearer(buyer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carId":"%s","offeredPrice":%d,"note":"Test teklifi"}
                                """.formatted(carId, price)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    /** İlanı güncelle (durum değiştirmek için). */
    protected void updateCar(TestUser seller, UUID carId, long price, String status) throws Exception {
        mockMvc.perform(put(API + "/car/" + carId)
                        .header("Authorization", bearer(seller.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":%d,"mileage":90000,"description":"Güncellendi","status":"%s"}
                                """.formatted(price, status)))
                .andExpect(status().isOk());
    }

    /**
     * Kafka consumer'ı e-postayı asenkron gönderdiği için, verilen adrese giden mail görünene kadar bekler.
     * Başka testlerden kalan mailler karışmasın diye alıcıya göre filtreler.
     */
    protected SimpleMailMessage awaitMailTo(String email) {
        return await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .until(() -> findMailTo(email), Objects::nonNull);
    }

    private SimpleMailMessage findMailTo(String email) {
        for (Invocation invocation : new ArrayList<>(Mockito.mockingDetails(mailSender).getInvocations())) {
            if (!invocation.getMethod().getName().equals("send")) {
                continue;
            }
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof SimpleMailMessage message && isTo(message, email)) {
                    return message;
                }
                if (arg instanceof SimpleMailMessage[] messages) {
                    for (SimpleMailMessage message : messages) {
                        if (isTo(message, email)) {
                            return message;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isTo(SimpleMailMessage message, String email) {
        return message.getTo() != null && Arrays.asList(message.getTo()).contains(email);
    }
}
