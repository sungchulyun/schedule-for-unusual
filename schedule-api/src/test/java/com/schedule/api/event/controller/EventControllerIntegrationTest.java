package com.schedule.api.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                                  "subjectType": "PERSONAL",
                                  "ownerUserId": "usr_me",
                                  "note": "정형외과"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ownerType").value("ME"))
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
}
