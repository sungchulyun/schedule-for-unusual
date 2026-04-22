package com.schedule.api.calendar.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void aggregatesMonthlyAndDateCalendarResponses() throws Exception {
        appUserRepository.save(new AppUser(
                "usr_me",
                OAuthProvider.KAKAO,
                "kakao-usr-me",
                "me",
                null,
                "grp_calendar",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_calendar")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "데이트",
                                  "startDate": "2026-04-18",
                                  "endDate": "2026-04-18",
                                  "subjectType": "SHARED",
                                  "ownerUserId": null,
                                  "note": "저녁 7시"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/shifts/2026-04-18")
                        .header("X-Group-Id", "grp_calendar")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shiftType": "DAY"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calendar/month")
                        .header("X-Group-Id", "grp_calendar")
                        .header("X-User-Id", "usr_me")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(4))
                .andExpect(jsonPath("$.data.filters.includeShifts").value(true))
                .andExpect(jsonPath("$.data.meta.groupId").value("grp_calendar"))
                .andExpect(jsonPath("$.data.events[0].title").value("데이트"))
                .andExpect(jsonPath("$.data.shifts[0].date").value("2026-04-18"))
                .andExpect(jsonPath("$.data.days[17].date").value("2026-04-18"))
                .andExpect(jsonPath("$.data.days[17].shift.shiftType").value("DAY"))
                .andExpect(jsonPath("$.data.days[17].events[0].title").value("데이트"));

        mockMvc.perform(get("/api/v1/calendar/date/2026-04-18")
                        .header("X-Group-Id", "grp_calendar")
                        .header("X-User-Id", "usr_me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2026-04-18"))
                .andExpect(jsonPath("$.data.meta.currentUserId").value("usr_me"))
                .andExpect(jsonPath("$.data.events[0].title").value("데이트"))
                .andExpect(jsonPath("$.data.shift.shiftType").value("DAY"));
    }

    @Test
    void appliesCalendarFiltersForOwnerTypesAndShiftVisibility() throws Exception {
        appUserRepository.save(new AppUser(
                "usr_filter",
                OAuthProvider.KAKAO,
                "kakao-usr-filter",
                "filter",
                null,
                "grp_calendar_filter",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        ));

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_calendar_filter")
                        .header("X-User-Id", "usr_filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "내 일정",
                                  "startDate": "2026-04-20",
                                  "endDate": "2026-04-20",
                                  "subjectType": "PERSONAL",
                                  "ownerUserId": "usr_filter"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Group-Id", "grp_calendar_filter")
                        .header("X-User-Id", "usr_filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "우리 일정",
                                  "startDate": "2026-04-20",
                                  "endDate": "2026-04-20",
                                  "subjectType": "SHARED"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/shifts/2026-04-20")
                        .header("X-Group-Id", "grp_calendar_filter")
                        .header("X-User-Id", "usr_filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shiftType": "NIGHT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/calendar/month")
                        .header("X-Group-Id", "grp_calendar_filter")
                        .header("X-User-Id", "usr_filter")
                        .param("year", "2026")
                        .param("month", "4")
                        .param("ownerTypes", "US")
                        .param("includeShifts", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filters.ownerTypes[0]").value("US"))
                .andExpect(jsonPath("$.data.filters.includeShifts").value(false))
                .andExpect(jsonPath("$.data.events.length()").value(1))
                .andExpect(jsonPath("$.data.events[0].title").value("우리 일정"))
                .andExpect(jsonPath("$.data.shifts.length()").value(0))
                .andExpect(jsonPath("$.data.days[19].events.length()").value(1))
                .andExpect(jsonPath("$.data.days[19].events[0].title").value("우리 일정"))
                .andExpect(jsonPath("$.data.days[19].shift").isEmpty());

        mockMvc.perform(get("/api/v1/calendar/date/2026-04-20")
                        .header("X-Group-Id", "grp_calendar_filter")
                        .header("X-User-Id", "usr_filter")
                        .param("ownerTypes", "ME")
                .param("includeShifts", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events.length()").value(1))
                .andExpect(jsonPath("$.data.events[0].title").value("내 일정"))
                .andExpect(jsonPath("$.data.shift").isEmpty());
    }
}
