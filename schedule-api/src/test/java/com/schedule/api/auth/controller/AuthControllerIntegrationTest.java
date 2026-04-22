package com.schedule.api.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.web.util.UriComponentsBuilder;
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
    void authenticatesWithKakaoMobileAccessTokenRefreshesAndLogsOut() throws Exception {
        given(kakaoOAuthClient.getUserProfileByAccessToken("mobile-access-token"))
                .willReturn(new KakaoUserProfile("kakao-123", "성철", "https://image.example/profile.png"));

        String callbackResponse = mockMvc.perform(post("/api/v1/auth/kakao/mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accessToken": "mobile-access-token"
                                }
                                """))
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

    @Test
    void supportsBrowserFallbackFlowAndExchangesLoginCode() throws Exception {
        given(kakaoOAuthClient.getUserProfile("mobile-code"))
                .willReturn(new KakaoUserProfile("kakao-mobile-123", "모바일유저", "https://image.example/mobile.png"));

        String loginLocation = mockMvc.perform(get("/api/v1/auth/kakao/login")
                        .param("appRedirectUri", "scheduleapp://auth/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("https://kauth.kakao.com/oauth/authorize")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String state = UriComponentsBuilder.fromUriString(loginLocation)
                .build()
                .getQueryParams()
                .getFirst("state");

        String callbackLocation = mockMvc.perform(get("/api/v1/auth/kakao/callback")
                        .param("code", "mobile-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("scheduleapp://auth/callback")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String loginCode = UriComponentsBuilder.fromUriString(callbackLocation)
                .build()
                .getQueryParams()
                .getFirst("loginCode");

        mockMvc.perform(post("/api/v1/auth/mobile/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "%s"
                                }
                                """.formatted(loginCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.oauthProvider").value("KAKAO"))
                .andExpect(jsonPath("$.data.user.nickname").value("모바일유저"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.tokens.accessToken").exists())
                .andExpect(jsonPath("$.data.tokens.refreshToken").exists());

        mockMvc.perform(post("/api/v1/auth/mobile/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginCode": "%s"
                                }
                                """.formatted(loginCode)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void rejectsBlankKakaoMobileAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/kakao/mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accessToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsUnsupportedAppRedirectUri() throws Exception {
        mockMvc.perform(get("/api/v1/auth/kakao/login")
                        .param("appRedirectUri", "https://malicious.example/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void redirectsBackToMobileAppWhenKakaoReturnsError() throws Exception {
        String loginLocation = mockMvc.perform(get("/api/v1/auth/kakao/login")
                        .param("appRedirectUri", "scheduleapp://auth/callback"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String state = UriComponentsBuilder.fromUriString(loginLocation)
                .build()
                .getQueryParams()
                .getFirst("state");

        mockMvc.perform(get("/api/v1/auth/kakao/callback")
                        .param("state", state)
                        .param("error", "access_denied")
                        .param("error_description", "user cancelled"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("scheduleapp://auth/callback")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("errorCode=AUTH_KAKAO_LOGIN_FAILED")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("error=access_denied")));
    }
}
