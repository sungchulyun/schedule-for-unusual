package com.schedule.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestCommonController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

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
                .andExpect(jsonPath("$.error.message").value("name: 이름을 입력해야 합니다."))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    void handlesMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/test/common/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("요청 본문을 읽을 수 없습니다."));
    }

    @Test
    void handlesTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/common/type-mismatch")
                        .param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("count: 요청 타입이 올바르지 않습니다."));
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
