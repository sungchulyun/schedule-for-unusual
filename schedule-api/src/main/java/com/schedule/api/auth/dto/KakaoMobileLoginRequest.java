package com.schedule.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoMobileLoginRequest(
        @NotBlank(message = "액세스 토큰을 입력해야 합니다.") String accessToken
) {
}
