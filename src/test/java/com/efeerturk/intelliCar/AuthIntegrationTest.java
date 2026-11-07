package com.efeerturk.intelliCar;

import com.efeerturk.intelliCar.enums.UserRole;
import com.efeerturk.intelliCar.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static String uniqueEmail() {
        return "auth-" + UUID.randomUUID() + "@test.local";
    }

    @Test
    void register_createsUserAndReturnsTokens() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", UserRole.SELLER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void register_withExistingEmail_isRejected() throws Exception {
        String email = uniqueEmail();
        String body = registerJson(email, "secret123", UserRole.BUYER);

        mockMvc.perform(post(API + "/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post(API + "/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withInvalidBody_isRejected() throws Exception {
        // Geçersiz e-posta ve 6 karakterden kısa şifre
        mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", "123", UserRole.BUYER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withCorrectPassword_returnsTokens() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", UserRole.BUYER)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"secret123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void login_withWrongPassword_isRejected() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", UserRole.BUYER)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get(API + "/car/my-cars"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpoint_withToken_isAllowed() throws Exception {
        TestUser user = registerUser(UserRole.SELLER);

        mockMvc.perform(get(API + "/car/my-cars").header("Authorization", bearer(user.token())))
                .andExpect(status().isOk());
    }

    @Test
    void refreshToken_returnsNewTokens() throws Exception {
        String email = uniqueEmail();
        MvcResult registered = mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", UserRole.BUYER)))
                .andExpect(status().isCreated())
                .andReturn();
        String refreshToken = JsonPath.read(registered.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(post(API + "/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        String email = uniqueEmail();
        MvcResult registered = mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "secret123", UserRole.BUYER)))
                .andExpect(status().isCreated())
                .andReturn();
        String json = registered.getResponse().getContentAsString();
        String accessToken = JsonPath.read(json, "$.accessToken");
        String refreshToken = JsonPath.read(json, "$.refreshToken");

        mockMvc.perform(post(API + "/auth/logout").header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        // Çıkıştan sonra eski refresh token ile yeni token alınamaz
        mockMvc.perform(post(API + "/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().is4xxClientError());
    }
}
