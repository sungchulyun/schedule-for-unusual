package com.schedule.api.legal.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schedule.api.auth.client.KakaoOAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = "app.support.email=help@example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KakaoOAuthClient kakaoOAuthClient;

    @Test
    void accountDeletionPageIsPublic() throws Exception {
        mockMvc.perform(get("/account-deletion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("linkTogether 계정 및 데이터 삭제 요청")))
                .andExpect(content().string(containsString("개발자 이름: 윤성철")))
                .andExpect(content().string(containsString("help@example.com")));
    }
}
