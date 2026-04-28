package com.schedule.api.notification.controller;

import com.schedule.api.notification.repository.FcmDeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FcmDeviceTokenRepository tokenRepository;

    @Test
    void registersAndUnregistersFcmToken() throws Exception {
        String token = "test-fcm-token";

        mockMvc.perform(post("/api/v1/notifications/fcm-token")
                        .header("X-Group-Id", "grp_notification")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "test-fcm-token",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registered").value(true));

        assertThat(tokenRepository.findById(token)).hasValueSatisfying(deviceToken -> {
            assertThat(deviceToken.getUserId()).isEqualTo("usr_me");
            assertThat(deviceToken.getGroupId()).isEqualTo("grp_notification");
        });

        mockMvc.perform(delete("/api/v1/notifications/fcm-token")
                        .header("X-Group-Id", "grp_notification")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "test-fcm-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(tokenRepository.findById(token)).isEmpty();
    }

    @Test
    void rejectsBlankFcmToken() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/fcm-token")
                        .header("X-Group-Id", "grp_notification")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "   ",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
