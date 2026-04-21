package com.schedule.api.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schedule.api.auth.client.KakaoOAuthClient;
import com.schedule.api.auth.client.KakaoUserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    @Test
    void authenticatesRefreshesAndLogsOut() throws Exception {
        given(kakaoOAuthClient.getUserProfile("valid-code"))
                .willReturn(new KakaoUserProfile("kakao-123", "성철", "https://image.example/profile.png"));

        String callbackResponse = mockMvc.perform(get("/api/v1/auth/kakao/callback")
                        .param("code", "valid-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.oauthProvider").value("KAKAO"))
                .andExpect(jsonPath("$.data.user.nickname").value("성철"))
                .andExpect(jsonPath("$.data.tokens.accessToken").exists())
                .andExpect(jsonPath("$.data.tokens.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = callbackResponse.split("\"accessToken\":\"")[1].split("\"")[0];
        String refreshToken = callbackResponse.split("\"refreshToken\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oauthProvider").value("KAKAO"))
                .andExpect(jsonPath("$.data.nickname").value("성철"));

        String refreshedResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = refreshedResponse.split("\"refreshToken\":\"")[1].split("\"")[0];

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedOut").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REVOKED"));
    }
}
