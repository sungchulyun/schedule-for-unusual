package com.schedule.api.shift.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShiftControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void upsertsAndReplacesAndDeletesShift() throws Exception {
        mockMvc.perform(put("/api/v1/shifts/2026-04-18")
                        .header("X-Group-Id", "grp_shift")
                        .header("X-User-Id", "usr_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shiftType": "DAY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shiftType").value("DAY"));

        mockMvc.perform(get("/api/v1/shifts")
                        .header("X-Group-Id", "grp_shift")
                        .header("X-User-Id", "usr_me")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].date").value("2026-04-18"));

        mockMvc.perform(put("/api/v1/shifts/monthly")
                        .header("X-Group-Id", "grp_shift")
                        .header("X-User-Id", "usr_partner")
                        .param("year", "2026")
                        .param("month", "4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "date": "2026-04-01",
                                      "shiftType": "DAY"
                                    },
                                    {
                                      "date": "2026-04-02",
                                      "shiftType": "NIGHT"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replacedCount").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(delete("/api/v1/shifts/2026-04-01")
                        .header("X-Group-Id", "grp_shift")
                        .header("X-User-Id", "usr_me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
    }
}
