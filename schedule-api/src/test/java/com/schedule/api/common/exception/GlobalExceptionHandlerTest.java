package com.schedule.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TestCommonController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/test/common/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("ok"))
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void handlesBusinessException() throws Exception {
        mockMvc.perform(get("/test/common/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("존재하지 않는 그룹입니다."))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    void handlesRequestBodyValidationException() throws Exception {
        mockMvc.perform(post("/test/common/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("name: must not be blank"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    void handlesConstraintViolationException() throws Exception {
        mockMvc.perform(get("/test/common/constraint")
                        .param("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("constraint.name: must not be blank"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    void handlesUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/common/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }
}
