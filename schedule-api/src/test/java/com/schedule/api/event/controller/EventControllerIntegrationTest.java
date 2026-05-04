package com.schedule.api.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void createsAndQueriesAndDeletesEvent() throws Exception {
        String response = mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_test")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "병원 방문",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "startTime": "09:30",
                                  "endTime": "10:30",
                                  "subjectType": "PERSONAL",
                                  "ownerUserId": "usr_me",
                                  "note": "정형외과"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ownerType").value("ME"))
                .andExpect(jsonPath("$.data.startTime").value("09:30:00"))
                .andExpect(jsonPath("$.data.endTime").value("10:30:00"))
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = response.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/v1/events")
                        .header("X-Group-Id", "grp_test")
                        .header("X-User-Id", "usr_me")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(eventId))
                .andExpect(jsonPath("$.data.items[0].title").value("병원 방문"));

        mockMvc.perform(patch("/api/v1/events/{eventId}", eventId)
                        .header("X-Group-Id", "grp_test")
                        .header("X-User-Id", "usr_partner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "병원 방문 후 약국",
                                  "note": "처방전 수령"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("병원 방문 후 약국"))
                .andExpect(jsonPath("$.data.updatedByUserId").value("usr_partner"));

        mockMvc.perform(delete("/api/v1/events/{eventId}", eventId)
                        .header("X-Group-Id", "grp_test")
                        .header("X-User-Id", "usr_me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void filtersEventsByOwnerType() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "내 일정",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "startTime": "09:00",
                                  "endTime": "10:00",
                                  "subjectType": "PERSONAL",
                                  "ownerUserId": "usr_me"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "우리 일정",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "startTime": "11:00",
                                  "endTime": "12:00",
                                  "subjectType": "SHARED"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events")
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("ownerTypes", "US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("우리 일정"))
                .andExpect(jsonPath("$.data.items[0].ownerType").value("US"));

        mockMvc.perform(get("/api/v1/events/date/2026-04-18")
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .param("ownerTypes", "ME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("내 일정"))
                .andExpect(jsonPath("$.data.items[0].ownerType").value("ME"));
    }

    @Test
    void rejectsBlankTitleUpdate() throws Exception {
        String response = mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "기존 일정",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "startTime": "09:00",
                                  "endTime": "10:00",
                                  "subjectType": "SHARED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = response.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(patch("/api/v1/events/{eventId}", eventId)
                        .header("X-Group-Id", "grp_filter")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("title must not be blank"));
    }

    @Test
    void rejectsPersonalEventOwnerOutsideCurrentGroup() throws Exception {
        appUserRepository.save(new AppUser(
                "usr_event_member",
                OAuthProvider.KAKAO,
                "kakao-event-member",
                "member",
                null,
                "grp_event_membership",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));
        appUserRepository.save(new AppUser(
                "usr_event_other",
                OAuthProvider.KAKAO,
                "kakao-event-other",
                "other",
                null,
                "grp_event_other",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_event_membership")
                        .header("X-User-Id", "usr_event_member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "다른 그룹 일정",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "startTime": "09:00",
                                  "endTime": "10:00",
                                  "subjectType": "PERSONAL",
                                  "ownerUserId": "usr_event_other"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("GROUP_ACCESS_DENIED"));
    }
}
