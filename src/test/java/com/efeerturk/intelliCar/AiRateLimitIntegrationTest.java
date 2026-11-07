package com.efeerturk.intelliCar;

import com.efeerturk.intelliCar.enums.UserRole;
import com.efeerturk.intelliCar.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI endpoint'lerinin kullanıcı başına rate limit'i, gerçek Redis ile.
 * Test profilinde limit dakikada 3'tür (application-test.properties). AI servisleri mock olduğu için Gemini'ye istek gitmez.
 */
class AiRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String RECOMMEND_BODY = "{\"budget\":500000}";
    private static final String PRICE_BODY = """
            {"brand":"Toyota","model":"Corolla","year":2018,"mileage":90000,"fuelType":"GASOLINE","transmission":"AUTOMATIC"}
            """;

    @Autowired
    private StringRedisTemplate redis;

    private void callRecommend(TestUser user, int expectedStatus) throws Exception {
        mockMvc.perform(post(API + "/ai/recommend")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RECOMMEND_BODY))
                .andExpect(status().is(expectedStatus));
    }

    private void callPriceEstimate(TestUser user, int expectedStatus) throws Exception {
        mockMvc.perform(post(API + "/ai/price-estimate")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRICE_BODY))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void requestsOverTheLimit_getTooManyRequestsWithRetryAfter() throws Exception {
        TestUser user = registerUser(UserRole.BUYER);

        for (int i = 0; i < 3; i++) {
            callRecommend(user, 200);
        }

        MvcResult denied = mockMvc.perform(post(API + "/ai/recommend")
                        .header("Authorization", bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RECOMMEND_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        long retryAfter = Long.parseLong(denied.getResponse().getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 60L);
    }

    @Test
    void bothAiEndpoints_shareTheSameQuota() throws Exception {
        TestUser user = registerUser(UserRole.BUYER);

        callRecommend(user, 200);
        callPriceEstimate(user, 200);
        callRecommend(user, 200);

        // 4. istek hangi AI endpoint'i olursa olsun reddedilir
        callPriceEstimate(user, 429);
    }

    @Test
    void limitIsPerUser() throws Exception {
        TestUser heavyUser = registerUser(UserRole.BUYER);
        TestUser otherUser = registerUser(UserRole.BUYER);

        for (int i = 0; i < 3; i++) {
            callRecommend(heavyUser, 200);
        }
        callRecommend(heavyUser, 429);

        callRecommend(otherUser, 200);
    }

    @Test
    void counterIsStoredInRedisWithExpiry() throws Exception {
        TestUser user = registerUser(UserRole.BUYER);

        callRecommend(user, 200);
        callRecommend(user, 200);

        String key = "ratelimit:ai:" + user.id() + ":minute";
        assertThat(redis.opsForValue().get(key)).isEqualTo("2");
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull().isBetween(1L, 60L);
    }

    @Test
    void anonymousRequests_areRejectedBeforeReachingTheLimiter() throws Exception {
        MvcResult result = mockMvc.perform(post(API + "/ai/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RECOMMEND_BODY))
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(429);
    }
}
