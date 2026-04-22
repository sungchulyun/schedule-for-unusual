package com.schedule.api.group.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schedule.api.auth.domain.AppUser;
import com.schedule.api.auth.domain.OAuthProvider;
import com.schedule.api.auth.domain.UserStatus;
import com.schedule.api.auth.repository.AppUserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GroupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void createsInviteAndAcceptsPartnerConnection() throws Exception {
        appUserRepository.save(new AppUser(
                "usr_owner",
                OAuthProvider.KAKAO,
                "kakao-owner",
                "owner",
                null,
                "grp_owner",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));
        appUserRepository.save(new AppUser(
                "usr_partner",
                OAuthProvider.KAKAO,
                "kakao-partner",
                "partner",
                null,
                "grp_partner",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/groups/me")
                        .header("X-Group-Id", "grp_owner")
                        .header("X-User-Id", "usr_owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("grp_owner"))
                .andExpect(jsonPath("$.data.members[0].role").value("OWNER"));

        String inviteResponse = mockMvc.perform(post("/api/v1/groups/invites")
                        .header("X-Group-Id", "grp_owner")
                        .header("X-User-Id", "usr_owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "KAKAO_TALK_SHARE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("grp_owner"))
                .andExpect(jsonPath("$.data.inviteId").exists())
                .andExpect(jsonPath("$.data.inviteCode").exists())
                .andExpect(jsonPath("$.data.inviteToken").exists())
                .andExpect(jsonPath("$.data.shareUrl").exists())
                .andExpect(jsonPath("$.data.deepLink").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String inviteCode = inviteResponse.split("\"inviteCode\":\"")[1].split("\"")[0];
        String inviteToken = inviteResponse.split("\"inviteToken\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/groups/invites/{inviteToken}", inviteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteId").exists())
                .andExpect(jsonPath("$.data.groupId").value("grp_owner"))
                .andExpect(jsonPath("$.data.inviter.userId").value("usr_owner"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requiresAuth").value(true));

        mockMvc.perform(post("/api/v1/groups/invites/accept")
                        .header("X-Group-Id", "grp_partner")
                        .header("X-User-Id", "usr_partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteToken": "%s"
                                }
                                """.formatted(inviteToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value("grp_owner"))
                .andExpect(jsonPath("$.data.inviteId").exists())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.members.length()").value(2))
                .andExpect(jsonPath("$.data.permissions.canEditAllEvents").value(true));

        mockMvc.perform(post("/api/v1/groups/partner")
                        .header("X-Group-Id", "grp_owner")
                        .header("X-User-Id", "usr_owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "%s"
                                }
                                """.formatted(inviteCode)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GROUP_INVITE_NOT_FOUND"));
    }

    @Test
    void rejectsUnsupportedInviteChannel() throws Exception {
        appUserRepository.save(new AppUser(
                "usr_owner_channel",
                OAuthProvider.KAKAO,
                "kakao-owner-channel",
                "owner-channel",
                null,
                "grp_owner_channel",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/groups/invites")
                        .header("X-Group-Id", "grp_owner_channel")
                        .header("X-User-Id", "usr_owner_channel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "SMS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("channel must be KAKAO_TALK_SHARE"));
    }
}
